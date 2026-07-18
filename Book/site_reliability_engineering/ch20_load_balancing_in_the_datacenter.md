# Chapter 20: Load Balancing in the Datacenter

Written by Alejandro Forero Cuervo
Edited by Sarah Chavis

> 스케일에서 load balancing은 표면엔 단순해 보이지만 — load balance early and load balance often — 어려움은 상세에 있다.

---

## 들어가며

이 장은 **datacenter 내부**의 load balancing에 집중. 구체적으로, 주어진 datacenter 내에서 query 흐름을 분산하는 알고리즘을 다룬다. 개별 서버로 요청을 라우팅하는 **application-level policies**를 다룸. 저수준 네트워킹 원리(switch, packet routing)와 datacenter 선택은 범위 밖.

### 전제 가정

- query의 흐름이 datacenter에 도착 — 자체 datacenter, 원격 datacenter, 또는 혼합 — 자원을 초과하지 않는 속도 (또는 극히 짧은 시간만 초과)
- datacenter 내에는 이 query가 작용하는 **services**가 있음
- 이 서비스들은 **동종, 교환 가능한 서버 프로세스**(대부분 다른 기계에서 실행)로 구현

| 서비스 규모 | backend 프로세스 수 |
|-------------|---------------------|
| 최소 | ≥ 3 (단일 기계 손실 시 50% 이상 capacity 손실 방지) |
| 최대 | > 10,000 (datacenter 크기에 따라) |
| 전형 | 100 ~ 1,000 |

- 이 프로세스들을 **backend task**(또는 그냥 backend)라 함
- backend task에 연결을 유지하는 다른 task를 **client task**라 함
- 각 수신 query마다 client task는 어느 backend task가 query를 처리할지 결정해야
- client는 TCP와 UDP 조합 위에 구현된 프로토콜로 backend와 통신

### Google datacenter의 현실

Google datacenter는 이 장에서 논의된 정책의 다양한 조합을 구현하는 매우 다양한 서비스를 수용. 작업 예시는 어떤 단일 서비스에 직접 맞지 않지만, 여러 서비스에 유용한 기법을 논의할 수 있게 하는 일반화된 시나리오.

이 기법들은 stack의 많은 부분에 적용. 예: 대부분의 외부 HTTP 요청이 **GFE (Google Frontend)** — HTTP reverse proxying 시스템 — 에 도달. GFE는 이 알고리즘들과 Chapter 19 알고리즘을 사용해 요청 payload와 metadata를 정보를 처리할 수 있는 application을 실행하는 개별 프로세스로 라우팅. 이 application들은 다시 같은 알고리즘으로 의존 인프라/서비스와 통신.

> 단일 수신 HTTP 요청이 여러 시스템에 잠재적으로 다양한 지점에서 높은 fan-out을 가지는 의존 요청의 긴 transitive chain을 trigger할 수 있음 — **의존성 스택이 상대적으로 깊어질 수 있음**.

---

## The Ideal Case (이상적 경우)

주어진 서비스의 부하가 모든 backend task에 **완벽히 분산**된 이상적 경우, 임의 시점에 가장 적게/많이 로드된 backend task가 정확히 같은 양의 CPU를 소비.

```
가장 로드된 task가 한계에 도달한 시점에만
datacenter에 더 이상 트래픽을 보낼 수 없음

→ 따라서 가장 로드된 task의 부하를 줄이는 것 = 전체 capacity 증가
```

이상적 경우에서는 load balancing이 필요 없다 — 모든 backend가 동등. 현실에서는 여러 요인이 불균형을 만든다.

---

## Simple Round Robin의 한계

### Request Cost의 다양성 (요청 비용 편차)

가장 싼 요청보다 **가장 비싼 요청이 100, 1,000, 심지어 10,000배 더 비쌈**. 인터페이스를 간단히 유지하기 위해 서비스는 종종 가장 비싼 요청이 가장 싼 것보다 훨씬 더 많은 자원을 소비하도록 정의.

예: Java backend의 경우, query가 평균 15ms CPU를 소비하지만 일부 query는 쉽게 10초까지 소비. backend가 여러 CPU 코어를 예약해 일부 계산이 병렬로 일어나 latency를 줄이지만, 큰 query 수신 시 부하가 몇 초간 크게 증가.

- 부적절하게 행동하는 task는 메모리 부족 또는 응답 정지 가능 (memory thrashing)
- 정상 경우라도 큰 query 완료 시 부하 정상화하지만, **다른 요청의 latency가 비싼 요청과의 자원 경쟁으로 고통**

Simple Round Robin은 client가 이 다양성을 인지하지 못하고 무작위로 분배 → 불운한 task가 비싼 요청을 더 많이 받음 → 불균형.

### Machine Diversity (기계 다양성)

같은 datacenter의 기계가 반드시 같지 않음 — CPU 성능이 다양. 따라서 같은 요청이 다른 기계에서 상당히 다른 작업량을 나타낼 수 있음.

> 이기종 자원 capacity를 다루는 이론적 해결책은 간단: 기계/프로세서 유형에 따라 CPU 예약을 scaling. but 실제로는 상당한 노력 필요.

Google은 **GCU (Google Compute Unit)** 라는 CPU 비율의 가상 단위를 만들어 해결:
- GCU가 CPU 비율 모델링의 표준
- 각 CPU 아키텍처를 평균 기계 성능을 기반으로 해당 GCU에 매핑
- job scheduler가 equivalence factor와 스케줄된 기계 유형에 따라 CPU 예약 조정

---

## Subsetting (부분집합 선택)

이상적으로 각 client task가 모든 backend에 연결할 수 있지만, 현실에서는 불가능:
- backend가 수천 → 각 client가 모든 backend에 연결 유지 시도 시 막대한 overhead
- backend가 client에게 응답하느라 시간 소비, 실제 query 처리 시간 감소
- connection setup, health check, 모든 backend의 configuration 변경 수신이 부하

**해결책:** 각 client는 backend의 **subset**에만 연결. subsetting 크기 선택은 trade-off:
- 너무 작으면 → 부하 분산 어려움, 단일 backend 실패 영향 큼
- 너무 크면 → connection overhead, connection churn

### Random Subsetting (무작위 부분집합)

naive 구현: 각 client가 backend 목록을 한 번 무작위 shuffle하고 목록의 시작부터 subset을 채움.

**실제로는 매우 나쁘게 작동** — 부하를 매우 불균일하게 분산.

#### 시뮬레이션 결과

| 설정 | 최소 부하 | 최대 부하 |
|------|-----------|-----------|
| 300 client / 300 backend, subset 30% (90 연결) | 63% (57 연결) | 121% (109 연결) |
| 300 client / 300 backend, subset 10% (30 연결) | 50% (15 연결) | 150% (45 연결) |

> 작은 subset 크기는 더 심한 불균형을 만듦. random subsetting이 부하를 비교적 균등하게 분산하려면 subset 크기가 75%까지 커야 — **비실용적**.

### Deterministic Subsetting (결정론적 부분집합) — Google의 해결책

```python
def Subset(backends, client_id, subset_size):
  subset_count = len(backends) / subset_size

  # Group clients into rounds; each round uses the same shuffled list:
  round = client_id / subset_count
  random.seed(round)
  random.shuffle(backends)

  # The subset id corresponding to the current client:
  subset_id = client_id % subset_count

  start = subset_id * subset_size
  return backends[start:start + subset_size]
```

**작동 방식:**
- client task를 **"round"**로 그룹 — round i는 `subset_count × i`에서 시작하는 연속 `subset_count`개의 client task로 구성
- `subset_count` = subset 수 (backend 수 / 원하는 subset 크기)
- **같은 round 내의 client는 같은 shuffled 목록 사용** (같은 random seed)
- 각 client는 자신의 `subset_id`에 따라 목록의 다른 부분 선택

**핵심 통찰:** random subsetting이 실패하는 이유는 각 client가 독립적으로 shuffle하므로 일부 backend가 여러 client에 의해 선택되고 일부는 무시됨. Deterministic subsetting은 **같은 round의 client들이 서로 다른 subset을 선택**하게 해 모든 backend가 정확히 같은 수의 client에 의해 선택되도록 보장.

> subset이 변경될 때(backend 추가/제거)에도 잘 작동 — client가 **서로 다른 round**에 속하게 되고, 그 round의 shuffled 목록을 사용.

#### Restart/Resize 처리

알고리즘은 client와 backend 수의 resize를 **최소 connection churn**으로 처리해야 하며, 이 수를 사전에 알 필요 없음. 이는 특히 (예: 새 버전 push를 위해) 전체 client/backend task 집합을 한 번에 하나씩 restart할 때 중요 (and 까다로움). backend가 push될 때 client가 최소 connection churn으로 계속 서빙하기를 원함.

---

## Load Balancing Policies (로드 밸런싱 정책)

### 1. Simple Round Robin

subset 내의 backend에 query를 차례로 순환 분배. 간단하지만 위에서 논의한 한계(request cost 편차, 기계 다양성)로 인해 실제로 부하를 잘 분산하지 못함.

### 2. Least-Loaded Round Robin

각 client가 자신의 **활성 요청(active request) 수**를 추적. 활성 요청이 가장 적은 backend를 선호.

#### Unhealthy Task와 Sinkholing 문제

unhealthy backend task는 보통 매우 빠르게 에러를 반환 — 정상 요청 처리보다 빠름. 결과: client가 unhealthy task를 가용한 것처럼 오인해 **매우 많은 트래픽**을 보냄. 이 unhealthy task가 이제 트래픽을 **sinkholing**하고 있다고 함.

**해결책:** 정책을 수정해 **최근 에러를 활성 요청처럼 count**. 이렇게 하면 backend task가 unhealthy해지면 load balancing 정책이 과부하 task에서 부하를 돌리는 것과 같은 방식으로 부하를 돌림.

#### 두 가지 중요 한계

1. **활성 요청 수 ≠ backend capability의 좋은 대리**
   - 많은 요청이 응답 대기(I/O)에 상당 부분을 소비, 실제 처리는 거의 안 함
   - 예: 한 backend가 다른 것보다 2배 많은 요청 처리 가능 (CPU 2배 빠름), but 요청 latency는 비슷 (네트워크 응답 대기가 대부분). I/O block은 CPU 0, RAM 거의 0, bandwidth 0 소비 → 2배 빠른 backend에 2배 요청 보내야 하지만 Least-Loaded는 두 task를 동등히 로드된 것으로 간주

2. **각 client는 자신의 요청만 봄**
   - 각 client task는 backend task 상태에 매우 제한된 view (자신 요청만)
   - 다른 client의 같은 backend에 대한 요청은 포함 안 됨

> 실제로 Least-Loaded Round Robin을 쓰는 대형 서비스는 **가장 로드된 backend가 가장 덜 로드된 것의 2배 CPU 사용** — Simple Round Robin만큼 나쁘게 수행.

### 3. Weighted Round Robin (가중 라운드 로빈)

Simple과 Least-Loaded를 개선한 중요 정책. **backend가 제공한 정보**를 결정 과정에 통합.

#### 작동 원리

- 각 client task는 subset 내 각 backend에 **"capability" score** 유지
- 요청을 Round-Robin 방식으로 분배, but client는 비율적으로 가중
- 각 응답(health check 응답 포함)에서 backend는 현재 관측된 **query/sec, error/sec, utilization(일반적으로 CPU 사용량)** 포함
- client는 현재 성공적으로 처리된 요청 수와 utilization cost를 기반으로 capability score 주기적 조정
- **실패한 요청은 미래 결정에 영향을 주는 패널티** 초래

#### 실제 효과

Weighted Round Robin은 실제로 매우 잘 작동, 가장/가장 적게 사용된 task 간 차이를 크게 감소.

> Figure 20-6: client가 Least-Loaded에서 Weighted Round Robin으로 전환한 시점 주변의 무작위 backend subset CPU 비율. 가장 적게/가장 많이 로드된 task 간 spread가 극적으로 감소.

---

## 핵심 정리

| 개념 | 핵심 |
|------|------|
| Datacenter LB 목적 | datacenter 내 backend task 간 query 분산 (application-level) |
| Backend task | 동종, 교환 가능 서버 프로세스 (3 ~ 10,000+, 전형 100~1,000) |
| 이상적 경우 | 모든 backend 동등 부하 → LB 불필요 |
| Request cost 편차 | 비싼 요청이 싼 것보다 100~10,000배 비쌈 → 불균형 |
| Machine diversity | CPU 성능 다양 → GCU로 정규화 |
| Subsetting | 각 client는 subset에만 연결 (connection overhead 회피) |
| Random subsetting | 부하 불균형 심함 (subset 10% → 50%~150%) |
| Deterministic subsetting | 같은 round는 같은 shuffled list, 서로 다른 subset → 균형 |
| Simple Round Robin | 간단 but cost 편차/기계 다양성에 취약 |
| Least-Loaded RR | active request 수 기반, but I/O block 시 capability 오인, client 자신만 봄 |
| Sinkholing | unhealthy task가 빠른 에러로 트래픽 흡수 → 에러 count로 해결 |
| Weighted Round Robin | backend 제공 정보(QPS, error, CPU)로 capability score → 실제 최고 성과 |

---

## 추가로 알면 좋은 내용

### Datacenter Load Balancing의 전체 그림

```
[Frontend LB - Chapter 19]
      ↓
Datacenter 도착
      ↓
[Datacenter LB - 이 장]
      ↓
Client task가 subset 선택 → subset 내 backend에 query 분배
      ↓
Backend task가 query 처리
```

### Subsetting의 연결 수학

```
backend 수 N, client 수 M, subset 크기 S (백분율 p = S/N)

이상적 연결 수 (각 backend): M × p
Random subsetting 표준편차: √(M × p × (1-p))
  → subset 작을수록 상대적 편차 큼

Deterministic subsetting: 각 backend 정확히 M × p 연결 보장
  (같은 round의 client가 서로 다른 subset 선택)
```

### Deterministic Subsetting의 round 구조

```
backend = [B0, B1, ..., B299] (300개)
subset_size = 90 → subset_count = 300/90 ≈ 3

Round 0: client 0,1,2 → seed(0)로 shuffle → 각자 다른 subset
Round 1: client 3,4,5 → seed(1)로 shuffle → 각자 다른 subset
Round 2: client 6,7,8 → seed(2)로 shuffle → 각자 다른 subset
...

→ 각 backend는 정확히 (client 수 / backend 수)만큼의 연결 보장
```

### Connection Draining (연결 배출)

backend task가 push/재시작을 위해 내려갈 때:
1. **Draining 상태로 전환** — 새 연결 거부, 기존 연결은 완료 허용
2. Client가 health check로 draining 감지 → 새 요청 다른 backend로
3. 기존 연결이 자연스럽게 종료되길 대기
4. 안전하게 종료

> subset 알고리즘은 backend 제거 시 최소 churn을 보장해야 — deterministic subsetting의 핵심 설계 목표.

### Health Check와 Load Balancing의 관계

- backend는 health check 응답에 capability 정보(QPS, error, CPU) 포함
- client는 health check를 정기 수행 → capability score 갱신
- unhealthy backend → sinkholing 방지 (에러 count)
- draining backend → 새 요청 차단

### GCU (Google Compute Unit)

```
CPU 성능 정규화 모델:

- 기계 X (느림): 2 CPU = 1.6 GCU
- 기계 Y (빠름): 2 CPU = 2.5 GCU
- job scheduler: equivalence factor로 CPU 예약 조정

→ 이기종 fleet에서 동일 "용량" 보장
→ AWS의 vCPU, GCP의 CPU 플랫폼 매핑과 유사 개념
```

### Sinkholing의 실제 영향

```
정상 backend: query 50ms 처리
unhealthy backend: 즉시 에러 반환 (5ms)

Least-Loaded (미수정):
- unhealthy의 active request = 0 (빠른 에러)
- client가 "가장 덜 로드됨" 판단 → 더 많은 트래픽 전송
- 악순환: unhealthy가 전체 트래픽 흡수

해결 (에러 count):
- 최근 에러를 active request처럼 취급
- unhealthy의 "활성+에러" = 높음 → 부하 돌림
```

### Weighted Round Robin의 capability score 계산

```
capability ≈ f(성공적 QPS, error rate, CPU utilization)

- 높은 QPS + 낮은 CPU = 높은 capability → 더 많은 요청
- 높은 error = 패널티 → 요청 감소
- 높은 CPU = 포화 → 요청 감소

주기적 갱신 (EWMA - 지수 가중 이동 평균 사용)
```

### gRPC와의 관계

Google 내부 RPC 시스템은 gRPC의 기반이 됨:
- client-side load balancing (이 장의 패턴)
- weighted round robin, subsetting
- health checking, connection draining
- L7 (application-level) load balancing

> gRPC의 `pick_first` vs `round_robin` 정책이 이 장의 개념의 단순화 버전. Google 내부는 더 정교한 Weighted RR 사용.

### Service Mesh와의 관계

이 장의 개념은 service mesh의 핵심:
- **client-side LB** = sidecar proxy (Envoy, Linkerd)
- **subset** = outlier detection, load balancing pool
- **Weighted RR** = load balancing policy (LEAST_REQUEST, RING_HASH 등)
- **health check** = active/passive health checks
- **GCU** = resource requests/limits 정규화

### 현대 산업 도구와의 매핑

| Google 개념 | 산업 대응물 |
|-------------|-------------|
| Deterministic subsetting | Envoy subset load balancing, Kubernetes EndpointSlice |
| Weighted Round Robin | gRPC weighted_round_robin, Envoy LEAST_REQUEST |
| GCU | Kubernetes resource units, AWS vCPU |
| Connection draining | Kubernetes terminationGracePeriod, Envoy drain |
| Health check | gRPC health checks, Kubernetes liveness/readiness |
| Client-side LB | gRPC, Envoy, Istio sidecar |

### 다른 장과의 연결

| 연결 | 내용 |
|------|------|
| Chapter 19 (Frontend LB) | datacenter 선택 후 이 장이 datacenter 내부 처리 |
| Chapter 21 (Overload) | load balancing 실패 시 overload — Weighted RR로 방지 |
| Chapter 22 (Cascading Failure) | 부하 분산 실패 → cascade 위험 |
| Chapter 18 (Software Eng in SRE) | 이 알고리즘을 SRE가 개발한 도구 |
| Chapter 6 (Monitoring) | capability score의 입력 = 모니터링 데이터 |
| Chapter 4 (SLO) | 부하 분산의 목표 = SLO 유지 |
| Chapter 8 (Release Engineering) | connection draining, 무중단 push |