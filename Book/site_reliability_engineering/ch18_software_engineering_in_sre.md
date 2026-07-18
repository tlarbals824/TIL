# Chapter 18: Software Engineering in SRE

Written by Dave Helstroom and Trisha Weir with Evan Leonard and Kurt Delimon
Edited by Kavita Guliani

> Google의 프로덕션 환경은 — 어떤 기준으로는 — 인류가 만든 가장 복잡한 기계 중 하나다.

---

## 들어가며

Google의 소프트웨어 엔지니어링 노력을 묻으면 대부분 Gmail이나 Maps 같은 소비자 제품을 댄다. 일부는 Bigtable이나 Colossus 같은 인프라를 언급할 것이다. 하지만 진실은, **소비자가 절대 보지 못하는 방대한 behind-the-scenes 소프트웨어 엔지니어링**이 존재한다. 그 제품들 중 상당수는 SRE 조직 내에서 개발된다.

SRE는 프로덕션의 복잡함을 직접 경험하므로, 프로덕션을 계속 가동하는 것과 관련된 내부 문제·유스케이스를 해결할 적절한 도구를 개발하는 데 **독보적으로 적합**하다.

이런 SRE 개발 도구의 특징:
- uptime 유지, latency 낮춤과 관련 — but 형태는 다양 (binary rollout 메커니즘, 모니터링, 동적 서버 구성 기반 개발 환경)
- one-off 해결책이나 빠른 hack이 아닌 **full-fledged software engineering project**
- SRE는 **product-based mindset** 채택 — 내부 고객과 미래 계획의 roadmap을 모두 고려

---

## Why Is Software Engineering Within SRE Important?

Google 프로덕션의 방대한 스케일이 내부 소프트웨어 개발을 필연적으로 만들었다 — Google 요구에 충분한 스케일로 설계된 third-party 도구가 거의 없기 때문. SRE 내에서 직접 개발하는 이점을 경험하며 학습했다.

### SRE가 내부 소프트웨어 개발에 적합한 이유

| 이유 | 설명 |
|------|------|
| **Google-specific 프로덕션 지식** | scalability, graceful degradation, 다른 인프라/도구와의 인터페이스를 고려한 설계 가능 |
| **subject matter에 embedded** | 개발 중인 도구의 요구사항·needs를 쉽게 이해 |
| **직접 사용자 관계** | 동료 SRE가 사용자 → frank, high-signal feedback. 빠른 launch & iterate 가능. 내부 사용자는 minimal UI 등 alpha 제품 문제에 더 관대 |

### 핵심 원칙: 팀 규모 ≠ 서비스 성장

> "team size should not scale directly with service growth"

지수적 서비스 성장에 맞춰 선형적 팀 성장을 달성하려면 **지속적 automation 작업**과 도구·프로세스·비효율을 제거하는 노력이 필요. 프로덕션 시스템을 직접 운영한 경험이 있는 사람들이 uptime·latency 목표에 기여할 도구를 개발하는 것이 합리적.

개별 SRE와 SRE 조직 모두 소프트웨어 개발에서 이익을 얻는다.

---

## Case Study: Auxon — Intent-Based Capacity Planning

이 장의 핵심 사례는 **Auxon**, linear programming 기반의 capacity planning 도구.

### Intent-Based Capacity Planning

Intent란 서비스 소유자가 서비스를 어떻게 운영하고 싶은지에 대한 rationale. concrete resource demand에서 동기가 되는 이유로 옮겨가려면 여러 추상화 계층이 필요.

**추상화의 사슬:**

1. *"cluster X, Y, Z에 service Foo용 core 50개를 원한다"*
   - explicit resource request. 하지만 왜 하필 이 클러스터들에?

2. *"지역 YYY의 임의 3개 cluster에 50-core footprint를 원한다"*
   - 자유도 증가, 더 충족하기 쉬움. but 여전히 이유 설명 없음

3. *"각 지역에서 service Foo의 수요를 충족하고 N+2 redundancy를 원한다"*
   - 훨씬 큰 유연성 도입. 요청이 미충족 시 무슨 일이 일어나는지 "인간" 수준에서 이해 가능

4. *"service Foo를 5 nines 신뢰성으로 운영하고 싶다"*
   - 더 추상적. 미충족 시 결과가 명확: 신뢰성 저하. N+2가 충분/최적이 아닐 수 있음

> 어느 수준의 intent를 써야 할까? 이상적으로 **모든 수준을 함께 지원**, 서비스가 intent를 더 명시할수록 더 큰 이익. Google 경험상 **step 3**에서 최적의 win — 좋은 자유도 + 이해 가능한 용어. 정교한 서비스는 step 4 목표.

### Precursors to Intent (Intent의 전제조건)

서비스의 intent를 캡처하려면?

- **Dependencies**: 서비스는 다른 인프라/사용자 서비스에 의존. 예: service Foo → storage service Bar, "Bar는 Foo로부터 30ms network latency 내"에 있어야 함. 이 요구사항은 Foo와 Bar 모두의 배치에 중요한 영향. 의존성은 중첩됨 — Bar → Baz(분산 storage), Qux(application management). Foo의 배치 가능 위치는 Bar에, Bar는 다시 Baz/Qux에 의존.
- **Performance metrics**: 각 서비스가 자원을 소비하는 방식 (CPU, 메모리, 디스크 등의 비율). 모델링에 필요.
- **Prioritization**: 자원이 부족할 때 어떤 서비스가 먼저, 어떤 것이 후순위? 비즈니스 중요도 기반.

### Bin Packing

Auxon은 linear programming을 사용해 자원을 클러스터에 **bin packing** — 서비스를 가능한 한 빽빽이 채워 넣어 유휴 자원 최소화. bin packing은 NP-hard이지만, 현대 알고리즘은 known optimal solution까지 도달 가능.

이를 통해 SRE는 SLO, 프로덕션 의존성, 서비스 인프라 요구사항 같은 고차원 우선순위에 집중할 수 있고, 자원을 수동으로 뒤지는 저차원 작업에서 벗어난다. 부수적 이익: computational optimization으로 intent → implementation 매핑이 훨씬 정밀 → **비용 절감**.

---

## 사례에서 얻은 교훈

### 1. Get Customer Buy-in (고객 동의 얻기)

도구를 만들기 전에 사용자(고객 SRE 팀)를 설득하고 참여시켜야. 도구가 가치 있음을 보여주고, 그들의 요구사항을 product에 반영.

### 2. Team Composition (팀 구성)

소프트웨어를 개발하려면 사실상 **product team**을 만드는 것. 필요 역할:
- **Product Manager** — 고객 옹호자 역할
- **Tech Lead / Project Manager** — agile 개발 프로세스 운영 경험

SRE 조직은 이런 역할의 경험이 부족할 수 있음 → product 개발팀에 training/coaching 요청, PM 컨설팅 시간 요청. 충분히 큰 기회라면 전담 인력 채용도 고려.

### 3. Stay Grounded in Production (프로덕션에 뿌리내리기)

> Auxon 팀은 제품을 개발하면서도 프로덕션 세계에 깊이 참여했다.

팀은 여러 Google 서비스의 on-call 로테이션을 유지하고, design 논의와 기술 리더십에 참여. 이런 지속적 상호작용으로 팀은 프로덕션 세계에 grounded되어 있었다 — **자신 제품의 소비자이자 개발자**. 제품이 실패하면 팀이 직접 영향. feature request는 자신의 일차 경험에서 비롯. 이는 제품 성공에 대한 강한 소유감과 SRE 내에서의 신뢰성·정당성을 부여.

### 4. Approximation (근사로 시작)

> 완벽과 순수에 집착하지 말 것, 특히 문제의 경계가 잘 알려지지 않았을 때. **Launch and iterate.**

복잡한 소프트웨어 엔지니어링은 불확실성에 직면. Auxon은 linear programming이 팀에게 미개척 영역이라 초기에 불확실성에 직면.

**해결책:** 처음엔 단순 휴리스틱을 적용하는 **"Stupid Solver"**를 구축. 진짜 optimal solution은 아니지만, 비전이 달성 가능함을 보여줌.

> approximation으로 개발을 가속할 때, 미래 향상과 approximation 재검토가 가능한 방식으로 작업해야. Auxon은 solver 인터페이스를 추상화해 내부를 나중에 교체 가능하게 함. 결국 unified linear programming 모델에 자신감이 생기자 Stupid Solver를 더 똑똑한 것으로 교체하는 것은 간단한 작업이었다.

### 5. Modular Design for Fuzzy Requirements (불확실한 요구사항을 위한 모듈 설계)

요구사항이 fuzzy할 때, 이를 소프트웨어를 **general하고 modular하게 설계**하는 인센티브로 사용.

예: Auxon은 자동화 시스템과 통합을 목표로 했지만, 당시 자동화 시스템 세계가 매우 유동적. 각 도구에 고유한 해결책을 설계하는 대신 **Allocation Plan을 universally useful하게** 설계 → 자동화 시스템이 자체 통합 지점을 처리. 이 "agnostic" 접근이 신규 고객 온보딩의 핵심이 됨 — 특정 도구로 전환 없이 Auxon 사용 시작 가능.

또한 modular 설계로 기계 성능 모델 구축 시 fuzzy 요구사항 처리. 미래 기계 성능 데이터는 불확실 → 모델을 교체 가능하게 설계.

---

## Getting There (도입 방법)

조직적 소프트웨어 개발을 SRE에 도입하고 싶다면? 프로덕션 지원에 집중한 SRE 조직에 소프트웨어 개발 모델을 어떻게 도입할까?

> 이 목표는 기술적 도전만큼이나 **조직 변화**다.

SRE는 팀원과 긴밀히 일하며 문제를 빠르게 분석·대응하는 데 익숙. 그래서 SRE의 자연스러운 본능 — 즉시 필요를 충족할 코드를 빠르게 짜는 것 — 에 역행해야.

### 작은 팀 vs 큰 조직

- **작은 SRE 팀** → ad hoc 접근이 문제 아닐 수 있음
- **조직이 성장하면** → ad hoc 접근은 스케일 안 함. 기능적이지만 좁거나 단일 목적의 소프트웨어 해결책이 나오고, 공유 불가 → **중복 노력과 시간 낭비** 필연

### 무엇을 달성하고 싶은가?

- 팀 내 더 나은 소프트웨어 개발 실천만 foster?
- 아니면 팀 간 사용 가능, 조직 표준이 될 수 있는 소프트웨어 개발?

후자는 큰 조직에서 **여러 해에 걸친 시간**이 필요. 여러 전선에서 다뤄져야 하지만 payback이 더 큼. Google 경험의 guideline:

#### 1. Create and Communicate a Clear Message

전략, 계획, 그리고 무엇보다 **SRE가 이 노력에서 얻는 이익**을 정의하고 전달.

> SRE는 회의적인 집단 (사실 회의론은 채용 시 특별히 찾는 특성). 초기 반응은 "너무 overhead" 또는 "절대 안 될 거야".

매력적 사례로 시작:
- 일관되고 지원되는 소프트웨어 해결책 → 신규 SRE ramp-up 가속
- 같은 작업을 수행하는 방법 수 감소 → 부서 전체가 단일 팀이 개발한 기술의 이익 → **지식과 노력의 팀 간 이동성**

> SRE가 "이 전략이 어떻게 작동할까"를 묻기 시작하면 (전략을 추구해야 할까 말까가 아니라), 첫 번째 허들을 넘긴 것이다.

#### 2. Evaluate Your Organization's Capabilities

SRE는 많은 기술을 갖지만, 제품을 빌드·출시한 팀 경험이 부족한 경우 흔함. 유용한 소프트웨어를 개발하려면 사실상 **product team**을 만드는 것.

필요 역할:
- 누가 **product manager** 역할 (고객 옹호자)?
- tech lead나 project manager가 **agile 개발 프로세스** 운영 기술/경험 있는가?

갭 채우기:
- product 개발팀에 agile 실천 확립 training/coaching 요청
- PM에게 컨설팅 시간 요청 → 제품 요구사항 정의, feature 우선순위 지원
- 충분히 큰 기회라면 전담 인력 채용

#### 3. Expect and Account for Mistakes

소프트웨어 개발을 도입하는 과정에서 실수는 필연. 조직이 학습하면서:
- 일부 노력은 실패 → 그로부터 학습
- 작은 시작, 점진적 확장
- 초기 성공 사례로 신뢰 구축

---

## 핵심 정리

| 개념 | 핵심 |
|------|------|
| SRE 내 소프트웨어 개발 | one-off hack이 아닌 full-fledged product, product mindset |
| 적합한 이유 | Google-specific 지식, embedded, 직접 사용자(SRE 동료) feedback |
| 핵심 원칙 | team size ≠ service growth → 지속적 automation |
| Intent-based planning | concrete resource → motivating reasons 추상화 (step 3가 최적 win) |
| Intent 전제조건 | dependencies, performance metrics, prioritization |
| Bin packing | linear programming으로 자원 빽빽 배치, 비용 절감 |
| 교훈 1 | 고객 동의(buy-in) 먼저 |
| 교훈 2 | product team 구성 (PM, tech lead, agile) |
| 교훈 3 | 프로덕션에 grounded (on-call 유지, 자신 제품의 소비자+개발자) |
| 교훈 4 | Approximation — Stupid Solver로 시작, 인터페이스 추상화 |
| 교훈 5 | Fuzzy 요구사항 → modular/general 설계, agnostic 접근 |
| 도입 | 조직 변화 + 기술 도전; 회의론 극복, 능력 평가, 실수 수용 |

---

## 추가로 알면 좋은 내용

### SRE가 개발한 도구의 예시

| 도구 범주 | 예시 |
|-----------|------|
| Binary rollout | 배포 자동화, progressive rollout |
| Monitoring | 내부 모니터링 시스템 |
| Development environment | 동적 서버 구성 기반 dev 환경 |
| Capacity planning | Auxon (intent-based) |
| Incident management | Outalator (Chapter 16) |

### Product Mindset의 핵심 요소

```
Product = 사용자(내부 고객) + Roadmap(미래 계획) + 유지보수 + 지원

- one-off 해결책: 즉시 문제 해결, 공유 불가, 유지보수 없음
- Product: 지속 가능, 팀 간 공유, roadmap 기반 발전, 지원 모델
```

### Linear Programming (선형 계획법)

Auxon의 핵심 기술:
- 목적 함수(objective)를 최적화 (예: 자원 활용도 최대, 비용 최소)
- 제약 조건(constraints) 하에서 (예: 의존성, 용량, SLO)
- NP-hard 문제지만 현대 solver가 known optimal 도달
- bin packing, job scheduling 등에 적용

### Intent 추상화의 일반화

이 개념은 capacity planning 외에도 적용 가능:
- **Infrastructure as Code (IaC)**: "50 VM" → "3개 region에 N+2"
- **Service mesh**: "이 서비스와 연결" → "latency < 50ms, retries 3"
- **Auto-scaling**: "CPU > 70% 시 scale out" → "SLO 유지"
- **Kubernetes**: declarative (intent) vs imperative (implementation)

> Kubernetes의 declarative YAML이 "intent-based" 접근의 대중적 사례.

### Stupid Solver / MVP 패턴

Auxon의 "Stupid Solver"는 **MVP(Minimum Viable Product)** 패턴과 동일:
1. 핵심 가치를 증명할 최소 버전 구축
2. 인터페이스 추상화 → 내부 구현은 나중에 교체
3. 자신감/데이터 축적 후 진짜 구현으로 전환
4. "완벽이 좋은의 적" 회피

### Internal Open Source 모델

SRE 개발 도구는 **내부 오픈소스**처럼 운영:
- 한 팀이 product owner
- 다른 팀이 사용자이자 기여자
- Roadmap은 사용자 요구 + 기여로 형성
- 이는 Google 내부 도구 생태계의 일반적 패턴

### DevOps와의 관계

이 장은 DevOps 원칙의 구체적 구현:
- 개발과 운영의 융합 (SRE가 직접 도구 개발)
- 자동화 우선 (team size ≠ service growth)
- 엔지니어링 실천을 운영에 적용
- Chapter 1의 "SRE = software engineer로 운영팀 설계"와 정렬

### 다른 장과의 연결

| 연결 | 내용 |
|------|------|
| Chapter 1 (Introduction) | SRE = software engineer로 ops 설계; 50% ops cap |
| Chapter 5 (Toil) | automation으로 toil 제거 = 소프트웨어 개발 |
| Chapter 7 (Automation) | SRE 개발 도구의 자동화 역할 |
| Chapter 6 (Monitoring) | SRE 개발 모니터링 도구 |
| Chapter 17 (Testing) | SRE 개발 도구도 동일한 테스팅 필요 |