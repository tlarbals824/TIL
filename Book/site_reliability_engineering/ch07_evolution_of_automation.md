# Chapter 7: The Evolution of Automation at Google

Written by Niall Murphy with John Looney and Michael Kacirek / Edited by Betsy Beyer

> "Besides black art, there is only automation and mechanization." — Federico García Lorca

---

## 들어가며

SRE에게 자동화는 **힘의 배수기(force multiplier)** 이지 만병통치약이 아니다. 무분별한 자동화는 해결만큼 많은 문제를 만들 수 있다. 소프트웨어 기반 자동화가 대부분의 경우 수동 운영보다 낫지만, **둘 다 필요 없는 상위 수준의 시스템 설계(자율 시스템)** 가 더 낫다.

자동화의 가치는 **무엇을 하는가**와 **신중하게 적용하는가** 모두에서 온다.

---

## The Value of Automation: 자동화의 가치

### 1. Consistency (일관성)

수백 번 반복하는 작업을 사람이 수행하면 매번 다르다. 일관성 부족 → 실수, 누락, 데이터 품질 문제, 신뢰성 문제. **잘 정의된 절차의 반복 실행**에서 일관성이 핵심 가치.

### 2. A Platform (플랫폼)

자동화는 일관성뿐 아니라 **확장 가능한 플랫폼**을 제공한다.
- 버그 수정이 코드 한 곳에서 영구적으로 적용됨
- 추가 작업을 더 쉽게 확장
- 사람이 적절히 수행하기 어려운 빈도·시간에 실행
- 성능 메트릭을 export해 이전에 알 수 없던 프로세스 세부사항 발견

자동화 없음 = 시스템 운영에 부과되는 **세금**.

### 3. Faster Repairs (빠른 복구)

일반적 장애를 자동 해결하는 자동화 → **MTTR 감소**. 개발자 velocity 증가 (문제 예방·사후 정리 시간 절약). 프로덕션에서 발견된 문제일수록 수정 비용이 크다.

### 4. Faster Action (빠른 행동)

인프라 상황에서 사람은 기계만큼 빠르게 반응하지 못한다. failover, 트래픽 전환 등이 잘 정의되면 "계속 실행 허용" 버튼을 사람이 누를 필요가 없다. Google의 많은 서비스는 **수동 운영 임계값을 이미 넘어섰다.**

### 5. Time Saving (시간 절약)

가장 자주 인용되지만 즉각 계산하기 어려운 이유. 한 번 자동화하면 **누구나** 실행 가능 — 운영자와 작업의 분리(decoupling)가 강력하다.

> "자동화할 수 없는 프로세스와 솔루션을 엔지니어링하면, 계속 사람의 피땀눈물로 기계를 먹여 살려야 한다." — Joseph Bironas

### Google SRE에게 특별한 가치

- **행성 규모** 서비스, 기계/서비스 핸드홀딩 여유 없음
- **균일한 프로덕션 환경** — API 없으면 직접 구축, 소스 코드 접근 가능
- 일관성, 신속성, 신뢰성이 대부분의 대화를 지배

모든 것을 자동화할 수는 없다. 빠른 프로토타입, 자동화 연동 미설계 시스템이 존재한다. Google은 **플랫폼을 만들 수 있는 위치**에 자신을 두는 것을 선호한다.

---

## The Use Cases for Automation: 자동화 사용 사례

자동화 = 소프트웨어에 작용하는 **메타 소프트웨어**.

| 사용 사례 | 예시 |
|----------|------|
| 사용자 계정 생성 | — |
| 클러스터 turnup/turndown | — |
| 소프트웨어/하드웨어 설치·해제 준비 | — |
| 새 소프트웨어 버전 롤아웃 | — |
| 런타임 설정 변경 | — |
| 의존성 변경 | 특수 케이스 |

### Google SRE의 초점

SRE의 주요 관심사는 **인프라 라이프사이클 관리** (데이터 품질 관리가 아님). 예: 새 클러스터에 서비스 배포.

**도구 스펙트럼:**
- Perl: POSIX 수준, 무제한 범위지만 추상화 낮음
- Puppet/Chef: 높은 수준 추상화, 추론 쉬움, **leaky abstraction** 시 체계적·반복적·비일관적 실패
- 실제 클러스터 push: 네트워크 실패, 머신 실패, 불일치 상태 등 — 대부분의 추상화가 처리 못함

---

## A Hierarchy of Automation Classes: 자동화 계층

이상적으로는 **외부 glue logic이 필요 없는** 시스템. glue logic 사용 사례(계정 추가, 시스템 turnup)를 애플리케이션 내부에서 직접 처리.

### 자동화 진화 5단계

| 단계 | 설명 |
|------|------|
| **1) No automation** | DB 마스터를 수동으로 위치 간 failover |
| **2) Externally maintained system-specific** | SRE 홈 디렉토리의 failover 스크립트 |
| **3) Externally maintained generic** | "generic failover" 스크립트에 DB 지원 추가 |
| **4) Internally maintained system-specific** | DB가 자체 failover 스크립트를 포함해 배포 |
| **5) Autonomous systems** | DB가 문제를 감지하고 사람 개입 없이 자동 failover |

**프로덕션 전체 범위 변경** (Chubby 서버 변경, Bigtable 클라이언트 플래그 등)도 별도 자동화 영역. 일정 볼륨 이상이면 수동은 불가능.

---

## Case Study 1: Automate Yourself Out of a Job

### 배경: Ads Database on Borg

2005~2008년 Ads MySQL은 성숙 상태. 2008년 MySQL을 Borg로 마이그레이션 목표:
1. **머신/레플리카 유지보수 완전 제거** — Borg가 task setup/restart 자동 처리
2. **bin-packing** — 여러 MySQL 인스턴스를 한 물리 머신에

### 문제: Borg task 이동

Borg task는 주 1~2회 자동 이동. 레플리카는 허용 가능했지만 **마스터 failover에 30~90분** 소요.
- 공유 머신 + 커널 업그레이드 재부팅 → 주당 여러 failover 예상
- 수동 failover → **최대 99% 가용성** (비즈니스 요건 미달)
- error budget 충족하려면 failover당 **30초 미만** 다운타임 필요 → 사람 의존 절차로는 불가능

### 해결: Decider

2009년 **Decider** 자동 failover 데몬 완성. 계획/비계획 failover 모두 **95%가 30초 미만**.

### 결과

| 지표 | 개선 |
|------|------|
| 운영 유지보수 시간 | **95% 감소** |
| 단일 DB task 장애 | 더 이상 사람 페이징 없음 |
| 스키마 변경 자동화 후 총 운영 비용 | **거의 95% 감소** |
| 하드웨어 | **약 60% 해제** (bin-packing) |

**교훈:** 수동 절차를 대체하는 것보다 **플랫폼을 제공**하는 것이 현명하다. 실패를 피하려는 최적화에서 **실패를 수용하고 빠르게 복구**하는 최적화로 전환.

---

## Case Study 2: Cluster Turnup Automation

### 초기: 신입 교육 도구

몇 달마다 신입 SRE + 몇 달마다 새 클러스터 turnup → turnup이 자연스러운 교육 도구.

**Turnup 단계:**
1. 데이터센터 전력·냉각 설비
2. 코어 스위치 설치·백본 연결
3. 초기 랙 설치
4. DNS, lock service, storage, compute 기본 서비스 구성
5. 나머지 랙 배포
6. 사용자 대면 서비스에 리소스 할당

4단계와 6단계가 극도로 복잡. 100개 이상 서브시스템, 복잡한 의존성.

**실제 사고:** Bigtable 클러스터가 첫 번째(로깅) 디스크 미사용 설정 → 1년 후 자동화가 "첫 디스크 미사용 = 스토리지 없음 = wipe 안전"으로 해석 → **페타바이트급 데이터 즉시 삭제**. 암묵적 "안전" 신호에 의존하면 위험.

### Prodtest: 불일치 감지

Python unit test 프레임워크를 확장해 **실제 서비스를 unit test**. 의존성 체인, 한 테스트 실패 시 즉시 중단.

- 클러스터별 팀 Prodtest 실행
- 테스트 상태 그래프로 어느 단계에서 실패했는지 시각화
- 다른 팀의 예상치 못한 misconfiguration → bug filing → Prodtest 확장

**성과:** PM이 클러스터 "go live" 시점 예측 가능. 6주+ 걸리던 network-ready → live traffic이 이해 가능해짐.

### Idempotent Fix: 1주 Turnup 미션

경영진 미션: 3개월 후 5개 클러스터가 같은 날 network-ready → **1주 안에 turnup**.

**진화:** "misconfiguration 찾기" → "misconfiguration 고치기"
- 각 test에 fix 페어링
- **Idempotent fix** — 15분마다 실행해도 클러스터 손상 없음
- 의존성 충족 시 자동으로 다음 테스트·fix 진행

**한계:** test → fix → retest 사이의 latency로 flaky test, 비-idempotent fix로 불일치 상태 가능.

### 특화(Specialization)의 함정

turnup 지연을 줄이기 위해 단일 "turnup team"이 각 팀의 자동화를 티켓으로 실행.

**단기:** competent, accurate, timely 자동화 달성
**장기:** 도메인 전문가가 아닌 사람이 자동화 유지 → 관련성·정확도 하락. 자동화 코드는 unit test 코드처럼 **유지하는 팀이 집착적으로 동기화**하지 않으면 죽는다.

**조직적 인센티브 왜곡:**
- turnup 속도가 주 업무인 팀 → 서비스 팀의 기술 부채 감소 인센티브 없음
- 자동화를 운영하지 않는 팀 → 자동화하기 쉬운 시스템 구축 인센티브 없음
- PM → 새 기능이 단순함·자동화보다 우선

> **가장 기능적인 도구는 보통 사용하는 사람이 작성한다.**

### Service-Oriented Cluster-Turnup

SSH 기반 자동화 → 보안 요구로 **Local Admin Daemon** (인증·ACL·RPC 기반)으로 전환.

**최종 아키텍처 (SOA):**
- 서비스 팀이 cluster turnup/turndown RPC를 처리하는 **Admin Server** 소유
- turnup 자동화가 network-ready 시 각 Admin Server에 RPC 전송
- **low-latency, competent, accurate** — 팀·서비스 수가 매년 2배로 늘어도 유지

---

## Borg: Warehouse-Scale Computer의 탄생

### 진화 과정

```
특정 목적 머신 + "master" 로그인
  → 머신 설명 파일 + parallel SSH
  → Python 스크립트 (서비스 관리, 로그 파싱)
  → 머신 상태 DB + 정교한 모니터링
  → Borg: 정적 host/port/job → 관리되는 리소스 바다
```

Borg의 핵심: 클러스터 관리를 **API 호출 가능한 중앙 코디네이터**로 전환.
- 배치 + 사용자 대면 task를 같은 머신에 스케줄링
- OS 업그레이드 자동·연속, SRE 노력 거의 불필요
- 수천 대의 머신이 매일 생성·소멸·수리 — **SRE 개입 없음**

### 은유: 단일 머신 → 글로벌 컴퓨터

| 단일 머신 | 클러스터 관리 |
|----------|-------------|
| 프로세스가 CPU 간 이동 | task가 다른 머신으로 reschedule |
| 디스크/RAM 추가 | cluster turnup = 추가 스케줄 가능 용량 |
| 대량 컴포넌트 실패 시 중단 | 글로벌 컴퓨터는 **자가 수리** 필수 (통계적으로 매초 수많은 실패) |

**자율 시스템으로 올라갈수록 자기 성찰(self-introspection) 능력이 필요.**

---

## Reliability Is the Fundamental Feature

자동화의 역설: 효과적인 자동화가 일상 활동을 대체하면서, **자동화 실패 시 사람이 시스템을 운영할 능력을 잃는다** (Air France 447 등 사례).

- 수동 조작이 항상 가능하다고 가정하지만, 시간이 지나면 **수동 조작을 허용하는 기능 자체가 사라진다**
- Google도 자동화가 해를 끼친 사례가 있지만, 많은 시스템에서 자동화/자율 행동은 **선택이 아닌 필수**
- **신뢰성이 근본 기능**이고, 자율적·회복력 있는 행동이 이를 달성하는 한 방법

---

## Automation: Enabling Failure at Scale

### Diskerase 사고

콜로 랙 해제(decommission) 자동화가 Diskerase 완료 후 실패 → 디버깅을 위해 **처음부터 재시작** → 빈 집합이 "전체(everything)" 특수값으로 해석 → **모든 콜로의 거의 모든 CDN 머신이 Diskerase** → 수분 내 전체 CDN 디스크 wipe.

- 외부적으로는 레이턴시 약간 증가 (좋은 용량 계획 덕분)
- **2일** 재설치 + **수주** 감사·sanity check·rate limiting·idempotent workflow 추가

> 자동화는 규모에서 실패를 **가능하게** 하기도 한다. 방어적 설계가 필수.

---

## Recommendations: 권고사항

**Google 규모가 아니어도 자동화할 가치가 있다:**
- 시간 절약 이상의 가치 (일관성, 플랫폼, MTTR)
- **설계 단계**에서 자율 운영을 고려하는 것이 가장 높은 레버리지
- 충분히 큰 시스템에 자율 운영을 사후 적용하기는 어렵다

**좋은 소프트웨어 엔지니어링 실천:**
- 결합도 낮은 서브시스템
- API 도입
- 부작용 최소화

---

## 핵심 정리

| 개념 | 핵심 |
|------|------|
| **자동화 가치** | 일관성, 플랫폼, 빠른 복구/행동, 시간 절약(분리) |
| **5단계 계층** | 수동 → 외부 특화 → 외부 범용 → 내부 특화 → 자율 |
| **Decider 사례** | 30초 failover → 운영 95% 감소, 하드웨어 60% 해제 |
| **Turnup 사례** | Prodtest → idempotent fix → SOA Admin Server |
| **Borg** | API 기반 클러스터 관리 → 자율 시스템 |
| **주의** | 자동화는 규모에서 실패를 확대할 수 있음 — idempotency, sanity check, rate limiting 필수 |