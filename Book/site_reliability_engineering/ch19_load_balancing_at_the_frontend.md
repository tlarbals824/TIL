# Chapter 19: Load Balancing at the Frontend

Written by Piotr Lewandowski
Edited by Sarah Chavis

> 단일 바구니에 모든 알을 담는 것은 재앙의 처방이다.

---

## 들어가며

Google은 매초 수백만 요청을 처리한다. 단일 컴퓨터로는 이 수요를 감당할 수 없다. 하지만 그런 슈퍼컴퓨터가 있다 해도, 단일 실패점에 의존하는 전략은 쓰지 않을 것이다 — 대규모 시스템에서 **단일 실패점 의존은 재앙의 처방**이다.

이 장은 **high-level load balancing** — datacenter 간 사용자 트래픽을 어떻게 분산하는지 — 에 집중한다. 다음 장(Chapter 20)은 datacenter 내부에서의 load balancing을 다룬다.

---

## Power Isn't the Answer (파워만으로는 부족하다)

논증을 위해 믿을 수 없을 만큼 강력한 기계와 절대 실패하지 않는 네트워크가 있다고 가정하자. Google 요구를 충족할까? **아니다.**

이상적 구성도 여전히 네트워킹 인프라의 물리적 제약에 묶인다:
- **빛의 속도**: 광케이블 통신 속도의 upper bound → 데이터가 이동해야 할 거리에 기반한 serving 속도 상한
- **단일 실패점**: 이상 세계에서도 단일 실패점 인프라 의존은 나쁜 생각

현실에서 Google은 수천 기계와 그보다 많은 사용자를 가지며, 많은 사용자가 한 번에 여러 요청을 보낸다. **Traffic load balancing**은 수많은 datacenter 기계 중 어느 것이 특정 요청을 처리할지 결정하는 방법이다.

이상적으로 traffic은 "최적" 방식으로 여러 네트워크 링크, datacenter, 기계에 분산된다. but "최적"이란 무엇인가? **단일 답은 없다** — 다양한 요인에 크게 의존:

- **계층적 수준**: global vs local
- **기술적 수준**: hardware vs software
- **traffic의 성격**

### 두 가지 트래픽 시나리오 예시

| 요청 | 중요 변수 | 최적 분산 |
|------|-----------|-----------|
| Search request | **Latency** | RTT로 측정한 가장 가까운 가용 datacenter |
| Video upload | **Throughput** | 현재 underutilized된 링크 (latency 희생) |

**Local level** (datacenter 내부): 건물 내 모든 기계가 사용자에게 동등히 멀고 같은 네트워크에 연결되어 있다고 가정 → 최적 부하 분산은 **자원 활용 최적화**와 **단일 서버 과부하 방지**에 집중.

물론 이는 크게 단순화한 그림. 현실에서는 훨씬 더 많은 고려사항이 최적 부하 분산에 영향.

---

## Load Balancing with DNS

DNS 기반 load balancing은 사용자 연결이 시작되기도 전에 부하를 분산하는 첫 번째 계층.

### DNS의 매개자(middleman) 문제

DNS 기반 load balancing은 몇 가지 심각한 한계를 야기:

1. **Nondeterministic reply paths (비결정론적 응답 경로)**
2. **Additional caching complications (추가 캐싱 복잡성)**
3. **Recursive resolver가 사용자 위치를 가림**

#### Recursive Resolution 문제

authoritative nameserver가 보는 IP 주소는 사용자 것이 아니라 **recursive resolver**의 것. 이는 심각한 제한 — resolver와 nameserver 간 최단 거리에 대해서만 응답 최적화 가능.

**해결책**: [Con15]가 제안한 **EDNS0 extension** — recursive resolver가 보낸 DNS 질의에 client subnet 정보 포함. 이렇게 하면 authoritative nameserver가 resolver 관점이 아닌 **사용자 관점**에서 최적인 응답 반환. 공식 표준은 아니지만 명백한 장점으로 가장 큰 DNS resolver(OpenDNS, Google)가 이미 지원.

#### 단일 Nameserver가 수많은 사용자를 서빙

단일 nameserver가 사무실 하나부터 대륙 전체까지 영역이 다양한 수천~수백만 사용자를 서빙할 수 있음. 예: 대형 국가 ISP가 전체 네트워크를 단일 datacenter에서 운영하면서 각 도시권에 네트워크 interconnect를 가짐 → ISP nameserver가 자기 datacenter에 최적인 IP 응답, 더 나은 네트워크 경로가 있어도.

#### Caching 문제

recursive resolver는 일반적으로 응답을 캐시하고 TTL 내에서 전달. 결과: 주어진 응답의 영향 추정이 어려움 — 단일 authoritative 응답이 단일 사용자 또는 수천 사용자에게 도달할 수 있음.

**두 가지 해결책:**
- 트래픽 변화를 분석해 알려진 DNS resolver 목록을 지속 갱신, 각 resolver 뒤의 사용자 기반 규모 추정 → 주어진 resolver의 잠재 영향 추적
- 각 추적 resolver 뒤 사용자의 지리적 분포를 추정해 최적 위치로 안내 가능성 증가

지리적 분포 추정은 사용자 기반이 넓은 지역에 분산 시 특히 까다로움. 이 경우 **다수 사용자를 위해 경험 최적화**하는 trade-off.

### "최적 위치"의 의미

가장 명백한 답은 **사용자에게 가장 가까운 위치**. but 추가 기준:
- 선택한 datacenter가 사용자 요청을 처리할 **충분한 capacity**가 있는지
- 선택한 datacenter와 네트워크 연결이 **건강한 상태**인지 (전력/네트워크 문제를 겪는 datacenter로 요청을 보내는 것은 이상적이지 않음)

> 다행히 authoritative DNS 서버를 traffic, capacity, 인프라 상태를 추적하는 글로벌 제어 시스템과 통합 가능.

### Caching → TTL 하한

authoritative nameserver는 resolver 캐시를 flush할 수 없으므로 DNS record는 상대적으로 **낮은 TTL**이 필요. 이는 DNS 변경이 사용자에게 전파되는 속도의 **하한**을 효과적으로 설정. 이 외에는 load balancing 결정 시 이 점을 염두에 두는 것 외에 할 수 있는 것이 거의 없음.

### DNS의 한계 요약

| 장점 | 단점 |
|------|------|
| 연결 시작 전 부하 분산 | resolver가 사용자 위치 가림 |
| 단순하고 효과적 | 캐싱 → 전파 지연 |
| — | 512바이트 제한(RFC 1035) → 단일 응답 내 주소 수 상한 |

> 모든 DNS 응답은 RFC 1035 [Moc87]이 정한 512바이트 제한 내에 들어야. 이는 단일 DNS 응답에 넣을 수 있는 주소 수의 상한을 설정하며, 그 수는 거의 확실히 서버 수보다 적음.

**결론:** DNS만으로는 frontend load balancing 문제를 해결하기에 충분하지 않음. DNS의 초기 계층 뒤에는 **Virtual IP address**를 활용하는 계층이 따라야.

---

## Load Balancing at the Virtual IP Address

**Virtual IP address (VIP)** 는 어떤 특정 네트워크 인터페이스에도 할당되지 않는다. 대신 여러 장치에 걸쳐 공유된다. 사용자 관점에서 VIP는 단일한 일반 IP 주소로 남는다.

이 방식의 이점:
- 구현 상세(특정 VIP 뒤의 기계 수)를 숨김
- 유지보수 용이 — 사용자 모르게 upgrade 예약, pool에 기계 추가 가능

### Network Load Balancer

VIP 구현의 가장 중요한 부분은 **network load balancer**라는 장치. balancer는 packet을 수신해 VIP 뒤의 기계 중 하나로 forward. 이 backend들이 요청을 추가 처리.

### Backend 선택 접근법

#### 1. Least Loaded (가장 적게 로드된 것 선호)

가장 직관적 — 항상 가장 덜 바쁜 backend 선호. 이론적으로 최적의 최종 사용자 경험(요청이 항상 가장 덜 바쁜 기계로). **but stateful protocol에서 빠르게 붕괴** — 요청 지속 시간 동안 같은 backend를 써야 함. balancer가 보낸 모든 연결을 추적해 후속 packet이 올바른 backend로 가게 해야 함.

#### 2. Connection ID 기반 Hash (Stateless 대안)

packet 일부로 **connection ID**를 만들어(해시 함수 + packet 정보 사용) backend 선택.

```
id(packet) mod N

- id: packet을 입력으로 connection ID를 생성하는 함수
- N: 설정된 backend 수
```

이 방식은 상태 추적이 필요 없지만, backend pool이 변경되면 connection churn이 큼.

### Consistent Hashing (일관성 해싱)

backend pool 변경 시 기존 연결에 미치는 영향을 최소화하기 위해 **consistent hashing** 사용. backend가 추가/제거되면 일부 연결만 재매핑. 시스템이 압박받을 때(예: 진행 중인 DoS 공격) consistent hashing으로 fallback, 평소에는 단순 연결 추적 사용.

### Packet Forwarding 방식

#### Network Address Translation (NAT)

모든 단일 연결을 tracking table에 항목으로 유지 → 완전히 stateless한 fallback 메커니즘을 가질 수 없음.

#### Layer 2 (Data Link) 수정

전달된 packet의 **destination MAC address** 변경. 상위 계층의 모든 정보를 그대로 두어 backend가 원래 source/destination IP 주소를 받음. backend가 **원래 송신자에게 직접 응답** 가능 — **Direct Server Response (DSR)**.

**DSR의 장점:**
- 사용자 요청이 작고 응답이 큰 경우(예: 대부분의 HTTP 요청) **엄청난 절약** — 트래픽의 극히 일부만 load balancer 통과
- load balancer 장치에 상태 유지 불필요

**DSR(layer 2)의 단점 (스케일 시):**
- 모든 기계(balancer + backend)가 data link 계층에서 서로 도달 가능해야 함
- 단일 broadcast domain에 모든 기계가 있어야 → 기계 수가 과도히 자라면 문제

> Google은 이 해결책을 상당히 예전에 넘어섰고, 대안을 찾아야 했다.

#### Packet Encapsulation (Google의 현재 해결책)

[Eis16] — network load balancer가 전달된 packet을 **Generic Routing Encapsulation (GRE)** [Han94]로 다른 IP packet 안에 넣고, backend의 주소를 destination으로 사용.

- backend는 packet을 수신해 외부 IP+GRE 계층을 벗기고, 내부 IP packet을 네트워크 인터페이스로 직접 전달된 것처럼 처리
- network load balancer와 backend가 같은 broadcast domain에 있을 필요 없음 — **경로만 있으면 별개 대륙에도 있을 수 있음**

**Packet encapsulation의 강력함과 대가:**
- 네트워크 설계·진화 방식에서 큰 유연성
- but **패킷 크기 팽창** — encapsulation이 overhead 도입 (IPv4+GRE의 경우 정확히 24바이트)
- packet이 가용 **Maximum Transmission Unit (MTU)** 크기를 초과 → **fragmentation** 필요

> Datacenter 내에서 더 큰 MTU 사용으로 fragmentation 회피 가능 — but 큰 Protocol Data Unit을 지원하는 네트워크 필요.

---

## 핵심 정리

| 개념 | 핵심 |
|------|------|
| Frontend LB 목적 | datacenter 간 사용자 트래픽 분산 |
| 단일 실패점 | 슈퍼컴퓨터 있어도 금지 (빛의 속도, SPOF) |
| "최적" | 단일 답 없음 — global/local, hw/sw, traffic 성격에 따라 |
| Latency vs Throughput | search→가까운 DC, video upload→여유풍 링크 |
| DNS LB | 연결 전 첫 계층, but resolver가 위치 가림, 캐싱, 512B 제한 |
| EDNS0 | client subnet 정보로 사용자 관점 최적화 |
| TTL 하한 | DNS 변경 전파 속도의 하한 설정 |
| VIP | 여러 장치 공유 IP, 구현 상세 숨김 |
| Network load balancer | packet 수신해 backend forward |
| Least loaded | 직관적 but stateful protocol에 붕괴, 상태 추적 필요 |
| Connection ID hash | stateless but pool 변경 시 churn |
| Consistent hashing | pool 변경 영향 최소화, DoS 시 fallback |
| DSR | backend가 원래 송신자에 직접 응답, layer 2 한계 |
| GRE encapsulation | Google 현 해결책, broadcast domain 자유, but MTU fragmentation |

---

## 추가로 알면 좋은 내용

### Frontend Load Balancing의 전체 계층

```
User → DNS (datacenter 선택) → VIP (network LB) → Datacenter (Chapter 20)
                                        ↓
                              [Backend pool]
```

1. **DNS**: 사용자에게 가장 가까운/가용한 datacenter의 VIP IP 반환
2. **VIP / Network LB**: 해당 datacenter 내의 여러 backend로 packet 분산
3. **Datacenter LB (Chapter 20)**: backend task 간 세부 분산 (application-level)

### Global vs Local Load Balancing

| 수준 | 범위 | 주요 고려 |
|------|------|-----------|
| Global (이 장) | datacenter 간 | latency, throughput, capacity, health |
| Local (Chapter 20) | datacenter 내부 | 자원 활용, 단일 서버 과부하 방지 |

### Anycast vs Unicast DNS

DNS load balancing의 두 접근:
- **Unicast**: 각 datacenter가 고유 IP, DNS가 사용자 위치에 따라 선택
- **Anycast**: 여러 datacenter가 같은 IP 공유, BGP 라우팅이 가장 가까운 곳으로 전달
  - 빠른 장애 조치 (BGP withdrawal)
  - but 라우팅 비대칭 문제, 일부 네트워크에서 불안정

### EDNS0 Client Subnet (ECS)

| 항목 | 내용 |
|------|------|
| 목적 | recursive resolver가 사용자 subnet 정보를 authoritative server에 전달 |
| 효과 | 사용자 관점 최적 응답 (resolver 관점이 아닌) |
| 지원 | Google, OpenDNS 등 대형 resolver |
| 표준 | 공식 표준 아님 (draft) |
| 프라이버시 | subnet 정보 노출 우려 → /24 수준으로 제한 관행 |

### DNS TTL 전략

```
낮은 TTL (예: 30초)
  + 빠른 장애 조치, 빠른 traffic 이동
  - resolver 부하 증가, 캐시 효율 저하

높은 TTL (예: 3600초)
  + resolver 부하 감소, 캐시 효율
  - 장애 시 전파 지연, traffic 이동 느림

→ Google은 상대적으로 낮은 TTL 사용 (trade-off)
```

### Consistent Hashing의 수학

전통적 해싱(`id mod N`)은 backend 수 N이 변하면 대부분의 매핑이 변경. **Consistent hashing**은:
- 해시 공간을 원형(ring)으로 배치
- 각 backend와 각 키를 ring 위 점으로 매핑
- 키는 ring에서 시계 방향으로 만나는 첫 backend에 할당
- backend 추가/제거 시 인접 키만 재매핑 → **평균 K/N개의 키만 이동**

> Dynamo, Cassandra, Riak 등 분산 저장소의 핵심 기술이기도 함.

### DSR (Direct Server Response) 실제 구현

- LVS (Linux Virtual Server)의 DR 모드가 대표적 오픈소스 구현
- backend의 loopback 인터페이스에 VIP 설정 (non-arp)
- balancer는 packet의 DST MAC만 변경, IP는 그대로 → backend가 직접 응답
- HTTP(요청 작음/응답 큼)에서 가장 효율적

### Google의 GFE (Google Frontend)

Chapter 20에서 언급되는 **GFE** — Google의 HTTP reverse proxying 시스템:
- 대부분의 외부 HTTP 요청이 GFE에 도달
- GFE는 Chapter 19 알고리즘(Frontend LB) + Chapter 20 알고리즘(Datacenter LB) 조합으로 요청을 개별 application process로 라우팅
- URL 패턴 → application 매핑 (각 팀이 제어)
- application은 다시 GFE 알고리즘으로 의존 서비스 호출 — **의존성 체인이 깊어질 수 있음**, 단일 HTTP 요청이 여러 시스템에 fan-out

### CDN과의 관계

Frontend load balancing은 CDN의 근간:
- DNS 기반 → 사용자에게 가까운 edge node 선택
- Anycast → 자동 라우팅
- DSR → edge node가 직접 응답
- Google의 GFE + frontend LB = 자체 CDN 역할

### 현대 산업 도구와의 매핑

| Google 개념 | 산업 대응물 |
|-------------|-------------|
| DNS LB | Route 53, Cloud DNS, NS1 |
| VIP / Network LB | AWS NLB, GCP TCP Proxy, HAProxy |
| DSR | LVS-DR, AWS NLB cross-zone |
| GRE encapsulation | VXLAN, GENEVE (overlay 네트워크) |
| Anycast | Cloudflare, AWS Global Accelerator |
| EDNS0 ECS | 대부분의 상용 DNS 서비스 |

### 다른 장과의 연결

| 연결 | 내용 |
|------|------|
| Chapter 20 (Datacenter LB) | datacenter 내부 backend task 분산 — 이 장의 직접 후속 |
| Chapter 21 (Overload) | frontend LB가 잘못된 곳으로 트래픽 보내면 overload 유발 |
| Chapter 6 (Monitoring) | datacenter health, capacity 추적 = 글로벌 제어 시스템 입력 |
| Chapter 3 (Risk) | 단일 실패점 회피 = 신뢰성 원칙 |
| Chapter 4 (SLO) | latency/throughput SLO가 최적 분산 결정 입력 |