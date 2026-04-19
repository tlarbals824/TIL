# SSE 서버 간 구독 시 주의점

Feature Flag를 도입하면서 SSE 서버 간 구독을 직접 구현하게 됐고, 예상치 못한 곳에서 스트림이 조용히 끊기는 문제를 마주쳤습니다. 그 고민의 과정과 해결 방법을 정리해보려 합니다.

---

## SSE란?

SSE(Server-Sent Events)는 서버가 클라이언트로 데이터를 단방향으로 스트리밍하는 HTTP 기반 프로토콜입니다. WebSocket과 달리 단방향이고, 일반 HTTP 연결을 그대로 사용합니다.

브라우저에서는 `EventSource` API가 W3C 스펙으로 자동 재연결을 내장하고 있습니다. 연결이 끊기면 기본 3초 후 자동으로 재구독합니다.

서버 간 구독에서는 이 모든 것을 **직접 구현**해야 합니다.

| | 브라우저 EventSource | 서버 간 (WebClient 등) |
|---|---|---|
| 자동 재연결 | ✅ 스펙 내장 | ❌ 직접 구현 필요 |
| 재연결 간격 조정 | `retry` 필드로 서버가 제어 | 직접 backoff 설정 |

---

## 상황 설정: Feature Flag와 SSE

결제 수단 가중치를 동적으로 조정하기 위해 Feature Flag를 도입하게 됐습니다. GrowthBook을 검토했지만, 제공하는 SDK가 클라이언트(브라우저) 중심으로 설계되어 있어 서버 간 구독 구조에는 직접 구현이 필요했습니다.

설정 변경을 실시간으로 반영해야 한다는 요구사항도 있었습니다. 폴링은 변경이 없는 경우에도 주기적으로 요청을 보내야 하고, 변경 감지까지 지연이 생깁니다. SSE를 선택한 이유입니다.

```
Feature Flag 서버 (upstream)
        │
        │  SSE 스트림 (flag 변경 이벤트)
        ▼
결제 서비스 (downstream)
        │
        ├─ 이벤트 수신 → 결제 수단 가중치 갱신
        └─ 연결 끊김 → ?
```

---

## "WebClient로 구독하면 되지 않나?"

SSE 구독 자체는 어렵지 않습니다. Spring WebClient로 스트림을 열고 `Flux`로 이벤트를 받으면 됩니다.

```kotlin
webClient.get()
    .uri("http://feature-flag-service/api/sse")
    .accept(MediaType.TEXT_EVENT_STREAM)
    .retrieve()
    .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})
    .subscribe { event -> handleFlagChange(event) }
```

연결이 끊기면 재연결하면 될 것 같습니다. `retryWhen`을 붙이면 될 것 같아 보입니다.

```kotlin
flux
    .retryWhen(
        Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3))
            .maxBackoff(Duration.ofSeconds(30))
    )
    .subscribe { event -> handleFlagChange(event) }
```

실제로 이것만으로는 충분하지 않습니다.

---

## 문제: onComplete 시 스트림이 조용히 끊긴다

`retryWhen`은 **에러(onError)** 가 발생했을 때만 동작합니다. 그런데 SSE 서버(특히 Spring `SseEmitter`)는 타임아웃이 되면 에러 없이 **정상 종료(onComplete)** 합니다.

이 경우 `retryWhen`은 발동하지 않습니다. 스트림이 정상 종료되면 구독 자체가 끝나버리고, 이후 이벤트는 아무것도 받지 못합니다. 로그에도 에러가 남지 않아 문제를 인지하기까지 시간이 걸립니다.

서버와 클라이언트 사이에 CDN이 있는 경우 이 문제가 더 자주 발생합니다. CDN은 자체적인 커넥션 타임아웃 정책을 갖고 있어, upstream 서버가 살아있더라도 CDN이 먼저 연결을 정상 종료시켜버리는 경우가 많습니다.

```
SSE 연결
   │
   ├─ onError    → retryWhen 발동 ✅
   └─ onComplete → retryWhen 발동 안 함 ❌ → 스트림 종료
```

---

## 근본 원인: retryWhen과 repeatWhen의 차이

Reactor에서 두 연산자는 서로 다른 종료 신호를 처리합니다.

- `retryWhen` → **onError** 신호를 받았을 때 재구독
- `repeatWhen` → **onComplete** 신호를 받았을 때 재구독

SSE는 에러 없이 정상 종료되는 경우가 많기 때문에 두 가지를 함께 걸어야 모든 케이스를 커버할 수 있습니다.

---

## 해결: retryWhen + repeatWhen + backoff

```kotlin
fun subscribe(): Flux<ServerSentEvent<String>> {
    return webClient.get()
        .uri("http://feature-flag-service/api/sse")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})
        .repeatWhen { companion ->
            companion.delayElements(Duration.ofSeconds(3))
        }
        .retryWhen(
            Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3))
                .maxBackoff(Duration.ofSeconds(30))
                .jitter(0.5)
        )
}
```

**동작 흐름:**
```
SSE 연결
   ├─ onError    → retryWhen (backoff 재연결)
   └─ onComplete → repeatWhen (3초 후 재구독)
```

`jitter(0.5)`는 여러 인스턴스가 동시에 재연결을 시도하지 않도록 랜덤 지연을 추가합니다. upstream 서버가 다운된 상황에서 여러 인스턴스가 일제히 재연결을 시도하면 장애를 더 악화시킬 수 있습니다.

### 재연결 구간의 이벤트 유실 보완: 폴링 병행

`repeatWhen`으로 재구독하더라도 스트림이 끊긴 구간 동안 발생한 Flag 변경은 받을 수 없습니다. SSE는 연결된 시점 이후의 이벤트만 수신하기 때문입니다.

이를 보완하려면 **재연결 시점에 최신 Flag 상태를 폴링으로 한 번 가져오는 방식**을 함께 적용하면 됩니다. 연결이 끊긴 동안의 변경도 재구독 직후 반영되므로, 유실 없이 최신 상태를 유지할 수 있습니다.

```kotlin
flux
    .repeatWhen { companion ->
        companion.delayElements(Duration.ofSeconds(3))
            .doOnNext { fetchLatestFlags() } // 재구독 직전 폴링으로 최신 상태 동기화
    }
    .retryWhen(
        Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3))
            .maxBackoff(Duration.ofSeconds(30))
            .jitter(0.5)
            .doAfterRetry { fetchLatestFlags() } // 에러 재연결 시에도 동기화
    )
```

SSE는 실시간 변경 전파를, 폴링은 재연결 구간의 유실 보완을 담당하는 형태로 역할이 분리됩니다.

---

## 트레이드오프

**무한 재연결은 upstream 서버에 부담이 됩니다.** `Long.MAX_VALUE`로 재시도 횟수를 설정하면 서버 장애 상황에서도 계속 재연결을 시도합니다. backoff와 maxBackoff로 간격을 제한하더라도, 인스턴스 수가 많다면 상당한 트래픽이 발생할 수 있습니다.

**onComplete와 onError를 구분해 대응하기 어렵습니다.** `repeatWhen`은 정상 종료 시 항상 재구독하므로, 의도적으로 스트림을 닫아야 하는 상황(서비스 종료 등)에서는 별도로 구독을 취소해야 합니다.

**연결 상태가 외부에서 보이지 않습니다.** 재연결이 반복되고 있어도 애플리케이션 입장에서는 정상처럼 보입니다. `doOnSubscribe`, `doOnComplete`, `doOnError` 훅으로 상태를 로깅하거나, Prometheus Counter로 재연결 횟수를 메트릭으로 노출해두면 이상 징후를 빠르게 파악할 수 있습니다.

```kotlin
.doOnSubscribe { log.info("SSE connected") }
.doOnComplete { log.info("SSE completed (will reconnect)") }
.doOnError { e -> log.warn("SSE error: ${e.message}") }
```

---

## 정리

| | retryWhen만 사용 | retryWhen + repeatWhen |
|---|---|---|
| onError 시 재연결 | ✅ | ✅ |
| onComplete 시 재구독 | ❌ | ✅ |
| CDN 타임아웃 대응 | ❌ | ✅ |
| 구현 복잡도 | 낮음 | 낮음 |

브라우저에서는 `EventSource`가 재연결을 알아서 처리해줍니다. 서버 간 구독에서는 그 역할을 직접 해야 합니다. `retryWhen`만 걸고 `repeatWhen`을 빠뜨리면 스트림이 조용히 끊기고, 에러 로그도 없어 원인을 찾기 어렵습니다. 두 가지를 반드시 함께 적용하시길 권장합니다.
