# Chapter 17: Testing for Reliability

Written by Alex Perry and Max Luebbe
Edited by Diane Bates

> "If you haven't tried it, assume it's broken."
>
> — Unknown

---

## 들어가며

SRE의 핵심 책임 중 하나는 유지하는 시스템에 대한 **confidence(신뢰도)를 정량화**하는 것이다. SRE는 고전적인 소프트웨어 테스팅 기법을 스케일이 큰 시스템에 맞게 적용한다.

Confidence는 두 가지로 측정된다:
- **과거 신뢰성**: 모니터링이 수집한 시스템 행동 데이터 분석
- **미래 신뢰성**: 과거 데이터로부터 예측

미래 신뢰성 예측이 유용하려면 다음 둘 중 하나가 성립해야 한다:

1. 사이트가 완전히 변하지 않음 (software release나 서버 플릿 변경 없음) → 미래 행동이 과거와 유사
2. 사이트의 모든 변경을 confident하게 설명 가능 → 분석이 각 변경의 불확실성을 허용

**Testing은 변경이 발생할 때 특정 equivalence(동등성) 영역을 보여주는 메커니즘**이다. 변경 전후로 통과하는 각 테스트는 분석이 허용해야 할 불확실성을 줄인다. 철저한 테스팅은 미래 신뢰성을 실용적으로 유용할 만큼 상세히 예측하게 해준다.

테스팅 양은 시스템의 신뢰성 요구사항에 달려 있다. 코드베이스의 테스트 커버리지가 높아질수록 불확실성이 줄고, 각 변경이 초래할 신뢰성 저하 가능성이 줄어든다. 적절한 테스트 커버리지가 있으면 신뢰성이 허용 한계 이하로 떨어지기 전에 더 많은 변경을 가할 수 있다.

---

## Testing과 MTTR의 관계

| 명제 | 의미 |
|------|------|
| 테스트 통과 | 신뢰성을 증명하지는 않음 |
| 테스트 실패 | 신뢰성 부재를 일반적으로 증명 |

모니터링 시스템은 버그를 발견할 수 있지만, reporting pipeline이 반응하는 속도만큼만 빠르다. **MTTR(Mean Time to Repair)** 은 운영팀이 rollback 등으로 버그를 고치는 데 걸리는 시간이다.

### Zero MTTR

테스팅 시스템이 **MTTR 0**으로 버그를 식별할 수 있다 — 시스템 수준 테스트가 subsystem에 적용되어, 모니터링이 잡을 것과 동일한 문제를 잡는 경우. 이 테스트는 push를 차단해 버그가 프로덕션에 도달하지 않게 한다 (소스 코드에서는 여전히 수정 필요).

- Push 차단으로 Zero MTTR 버그를 수정하는 것은 빠르고 편리
- Zero MTTR로 잡을 수 있는 버그가 많을수록 사용자가 경험하는 **MTBF(Mean Time Between Failures)** 증가

MTBF가 증가하면 개발자는 feature를 더 빨리 release하도록 장려된다. 일부 feature는 당연히 버그를 가진다. 새 버그는 opportunity cost를 만들고... (이 사이클이 반복)

---

## Testing Roles (개발자 vs SRE)

| 역할 | 주체 | 초점 |
|------|------|------|
| Unit test | 개발자 | 개별 함수/모듈 |
| Integration test | 개발자 | 컴포넌트 간 상호작용 |
| System test | 개발자/SRE | 전체 시스템 |
| Release test | SRE | 변경이 안전한지 push 전 검증 |
| Regression test | 공통 | 과거 버그 재발 방지 |

엔지니어가 (주어진 시스템에 대해) 적절한 테스트를 일반화된 방식으로 정의하면, 남은 작업은 모든 SRE 팀에 공통이므로 **공유 인프라**로 간주된다. 이 인프라는 두 구성요소로 이루어진다:

- **Scheduler**: 서로 무관한 프로젝트에 예산된 자원을 공유
- **Executors**: 테스트 바이너리를 sandbox해 신뢰할 수 없는 것으로 취급되지 않게 함

이 두 인프라 컴포넌트는 cluster 규모 storage처럼 일반적인 SRE 지원 서비스로 다뤄진다.

---

## Testing Culture

### Smoke Tests로 시작

명백히 깨진 소프트웨어를 출시하는 것은 개발자의 가장 큰 죄 중 하나다. 모든 release마다 실행할 smoke test를 만드는 것은 **적은 노력, 큰 효과**의 첫걸음이다.

### Bug → Test 변환

강력한 테스팅 문화를 만드는 한 방법: **보고된 모든 버그를 test case로 문서화**. 각 테스트는 버그가 아직 수정되지 않았으므로 처음엔 실패해야 한다. 엔지니어가 버그를 수정하면 소프트웨어가 테스트를 통과하고, 종합적인 regression test suite로 나아가는 길이다.

### Build 인프라

- **Versioned source control**: 코드베이스의 모든 변경 추적 — 강력한 테스팅 인프라의 기초

- **Continuous build system**: 코드 제출 시마다 빌드 + 테스트 실행. 변경이 프로젝트를 깨뜨리는 순간 엔지니어에게 통지
- **"latest version은 완전히 작동해야 한다"**: 빌드가 깨지면 엔지니어는 다른 모든 작업을 중단하고 수정 우선순위

**결함을 심각하게 다루는 이유:**
- 결함 도입 후 코드베이스가 변경되면 수정이 더 어려움
- 깨진 소프트웨어는 팀을 느리게 함 (workaround 필요)
- Nightly/weekly 빌드 cadence가 가치를 잃음
- 긴급 release(예: 보안 취약점 공개) 대응이 복잡해짐

> 안정성과 민첩성은 전통적으로 긴장 관계지만, 빌드가 견고할 때 개발자는 더 빨리 iterate할 수 있다 — **안정성이 민첩성을 이끈다**.

### Bazel과 의존성 그래프

[Bazel](https://github.com/google/bazel) 같은 빌드 시스템은 소프트웨어 프로젝트의 의존성 그래프를 생성. 파일이 변경되면 그 파일에 의존하는 부분만 rebuild → **재현 가능한 빌드**. 모든 제출마다 전체 테스트 대신, 변경된 코드의 테스트만 실행 → 더 싸고 빠름.

### Test Coverage 측정

test coverage를 정량화·시각화하는 도구 [Cra10]를 사용해 테스팅의 초점을 형성. "테스트가 더 필요해" 같은 모호한 반복 대신 **명시적 목표와 deadline**을 설정. 단, 모든 소프트웨어가 동등하지 않음 — life-critical/revenue-critical 시스템은 수명이 짧은 비프로덕션 스크립트보다 훨씬 높은 테스트 품질·커버리지를 요구.

---

## Testing at Scale

### 테스트 종류

| 종류 | 특징 |
|------|------|
| Unit test | 개별 모듈, 빠름 |
| Integration test | 컴포넌트 간 상호작용 |
| System test | 전체 시스템, 프로덕션 유사 |
| Semantic/Sequence test | 순서 의존 동작 |
| Release test | push 전 안전성 검증 |
| Regression test | 과거 버그 재발 방지 |
| Hermetic test | 격리된 환경, 외부 의존 없음 |

### Using Statistical Tests (통계적 테스트)

[Lemon](https://google.github.io/dagger/)(fuzzing), [Chaos Monkey](https://github.com/Netflix/SimianArmy/wiki/Chaos-Monkey), [Jepsen](https://github.com/aphyr/jepsen) 같은 통계 기법은 반드시 재현 가능한 테스트는 아니다. 코드 변경 후 재실행해도 관측된 결함이 수정됐다고 확정할 수 없다.

하지만 유용한 점:
- 수행된 무작위 행동의 로그를 제공 (때로 random seed만 로깅)
- 이 로그를 release test로 refactor하면, 버그 리포트 작업 전 몇 번 실행하는 것이 도움
- 재생 시 **비실패율**이 얼마나 되는지 → 결함 수정 주장이 얼마나 어려울지 판단
- 결함 표현의 변화가 코드의 의심 영역을 좁혀줌
- 이후 실행에서 더 심각한 실패 상황이 드러날 수 있음 → 버그 심각도 escalate

### The Need for Speed

코드 저장소의 모든 버전(패치)마다 정의된 테스트는 pass/fail을 제공. 동일해 보이는 반복 실행에서도 결과가 달라질 수 있다. 실제 pass/fail 확률은 여러 실행의 평균으로 추정할 수 있지만, 모든 테스트·모든 버전에 대해 이 계산을 하는 것은 **계산적으로 비실용적**.

대신 관심 시나리오에 대한 가설을 세우고, 각 테스트·버전을 합리적 추론이 가능한 충분한 횟수로 반복 실행. 일부는 benign, 일부는 actionable. 이들은 서로 coupled되어, 빠르고 신뢰하게 actionable 가설(실제 깨진 컴포넌트) 목록을 얻으려면 **모든 시나리오를 동시에 추정**해야 한다.

### Testing Deadlines

| 테스트 유형 | 실행 시간 | 피드백 시점 |
|-------------|-----------|-------------|
| Simple (hermetic, small container, 수 초) | 빠름 | interactive — 엔지니어가 다음 bug/task로 넘어가기 전 |
| Batch (여러 binary/fleet orchestration, 수 초 startup) | 느림 | code reviewer에게 "이 코드는 review 준비 안 됨" |

테스트의 **비공식적 deadline**은 엔지니어가 다음 context switch를 하는 시점. 결과는 context switch 전에 주는 것이 최적 — 그렇지 않으면 다음 context가 [XKCD compiling](http://xkcd.com/303/)이 될 수 있다.

> 21,000개 이상의 simple test를 가진 서비스도 있다. 스케일에서 테스트 관리 자체가 엔지니어링 과제.

---

## Configuration Files

설정 파일이 존재하는 이유는, 설정 변경이 도구 재빌드보다 빠르기 때문. 이 낮은 latency는 종종 MTTR을 낮게 유지하는 요인. 하지만 같은 파일이 그 낮은 latency가 필요 없는 이유로도 자주 변경된다. 신뢰성 관점에서:

- **MTTR 낮추는 용도의 config**(실패 시에만 수정) → release cadence가 MTBF보다 느림. 수동 편집이 실제 최적인지 불확실
- **application release보다 자주 변경하는 config**(예: release state 보관) → 이 변경을 application release처럼 다루지 않으면 **주요 위험**. 테스트/모니터링 커버리지가 사용자 application보다 훨씬 좋지 않으면, 그 파일이 사이트 신뢰성을 부정적으로 지배

**권장:** 각 config 파일을 위 분류 중 하나로만 분류하고 규칙을 강제. 후자 전략 시:
- 각 config 파일에 정기적 편집을 지원하는 충분한 테스트 커버리지
- release 전, 테스트 대기 중 편집은 약간 지연
- **break-glass 메커니즘** 제공 — 테스트 완료 전 파일을 live push. break-glass는 신뢰성을 손상하므로 noisy하게 만들 것 (예: 다음 번 더 견고한 해결을 요청하는 bug filing)

### Break-Glass와 Testing

break-glass로 release testing을 비활성화하면, 서두른 수동 편집자는 모니터링이 실제 사용자 영향을 보고할 때까지 실수를 통보받지 못한다.

**더 나은 방법:** 테스트를 계속 실행하고, early push event를 pending testing event와 연관시키고, (가능한 한 빨리) push를 깨진 테스트로 back-annotate. 이렇게 하면 결함 있는 수동 push 뒤에 (hopefully 덜 결함 있는) 또 다른 수동 push가 빠르게 뒤따를 수 있다. 이상적으로 break-glass 메커니즘이 해당 release test의 우선순위를 자동 높여, test 인프라가 이미 처리 중인 routine incremental validation/coverage 작업을 선점하게 함.

### Integration Testing for Configuration

config 파일을 unit test하는 것 외에 **integration testing**도 중요. config 내용은 (테스팅 관점에서) config를 읽는 interpreter에게 잠재적으로 hostile content다. Python 같은 인터프리터 언어가 config에 자주 쓰이는 이유 — 인터프리터를 embed할 수 있고, 비악의적 코딩 에러로부터 보호하는 간단한 sandboxing이 가능.

하지만 인터프리터 언어로 config를 작성하는 것은 위험하다 — 잠복 결함이 많고 확실히 해결하기 어렵다. loading content가 실제로 프로그램 실행이므로, loading이 얼마나 비효율적일 수 있는지에 대한 **본질적 upper limit가 없다**. 모든 다른 테스팅과 함께 이런 integration testing은 모든 integration test 메서드에 신중한 deadline checking을 짝지어, 합리적 시간 내 완료되지 않는 테스트를 실패로 표시해야 한다.

**대안들:**
- **커스텀 문법의 text config** → 모든 테스트 범주를 처음부터 별도 커버
- **YAML + 잘 테스트된 parser**(Python `safe_load`) → config toil 일부 제거, loading 시간 hard upper limit 보장. but schema 결함은 구현자가 처리해야 하며 대부분의 단순 전략은 runtime upper bound가 없고 robustly unit test되지 않음
- **Protocol Buffers** → schema를 미리 정의하고 load 시 자동 검사 → toil 더 제거, bounded runtime 유지

### Defense in Depth

SRE 역할은 (아무도 안 쓰면) 시스템 엔지니어링 도구를 작성하고 robust validation과 테스트 커버리지를 추가하는 것을 포함. 모든 도구는 테스트가 잡지 못한 버그로 예상치 못하게 행동할 수 있으므로 **defense in depth**가 권장된다.

한 도구가 예상치 못하게 행동할 때, 엔지니어는 다른 대부분의 도구가 올바르게 작동해 부작용을 완화·해결할 수 있다고 최대한 confident해야 한다. **각 예상된 형태의 misbehavior를 식별하고, 어떤 테스트(또는 다른 도구의 테스트된 input validator)가 그 misbehavior를 보고하게 만드는 것**이 사이트 신뢰성 전달의 핵심 요소. 문제를 찾은 도구가 고치거나 멈출 수 없더라도, 치명적 outage 전에 문제를 보고해야 한다.

> 예: `/etc/passwd` 편집이 파서를 절반만 파싱하고 멈추게 하는 경우. 최근 생성 사용자가 load되지 않았어도 기계는 계속 실행될 가능성이 크고 많은 사용자가 결함을 눈치채지 못할 수 있다. home directory를 유지하는 도구가 실제 directory와 (부분적) 사용자 목록이 시사하는 것 간의 불일치를 쉽게 눈치채고 긴급 보고할 수 있다. 이 도구의 가치는 문제 보고에 있으며, 스스로 remediate(많은 사용자 데이터 삭제)하려 해서는 안 된다.

---

## Production Probes

Testing은 **known data**에 대한 허용 가능한 행동을 명시하고, monitoring은 **unknown 사용자 data**에 대한 허용 가능한 행동을 확인하므로, 알려진 것과 알려지지 않은 것 — 주요 위험 원천 — 모두가 testing + monitoring 조합으로 커버되는 것처럼 보인다. 불행히도 실제 위험은 더 복잡하다.

- **Known good requests** → 작동해야 함
- **Known bad requests** → error해야 함

두 종류 커버리지를 integration test로 구현하는 것이 일반적으로 좋은 생각. 같은 테스트 요청 묶음을 release test로 replay 가능. known good requests를 (a) 프로덕션에 replay 가능한 것과 (b) 불가능한 것으로 나누면 세 묶음이 된다:

1. Known bad requests
2. Known good requests — 프로덕션에 replay 가능
3. Known good requests — 프로덕션에 replay 불가능

각 묶음을 integration test와 release test 양쪽으로 사용 가능.

### Canary / Phased Rollout

| 기준 규칙(경험적) | 비율 |
|------------------|------|
| 시작 | 0.1% 트래픽 |
| 24시간마다 | 한 자릿수 위로 scaling (지리적 위치 변화) |
| Day 2 | 1% |
| Day 3 | 10% |
| Day 4 | 100% |

---

## 핵심 정리

| 개념 | 핵심 |
|------|------|
| Confidence 정량화 | 과거(모니터링) + 미래(예측) 신뢰성 |
| Testing의 역할 | 변경 시 equivalence 영역 입증 → 불확실성 감소 |
| Zero MTTR | 테스트가 모니터링과 같은 결함을 push 전 차단 → MTBF 향상 |
| Test infrastructure | scheduler(자원 공유) + executors(sandbox) = 공유 인프라 |
| Testing culture | bug→test 변환, continuous build, "latest는 작동해야" |
| Bazel | 의존성 그래프 기반 부분 rebuild, 재현 가능 빌드 |
| Statistical tests | Lemon, Chaos Monkey, Jepsen — 재현성은 아니지만 로그·비실패율로 통찰 |
| Test deadlines | simple→interactive, batch→code review; context switch 전 피드백 |
| Config files | MTTR용(느린 cadence) vs 자주 변경(위험); break-glass는 noisy하게 |
| Protocol Buffers | bounded runtime, schema 자동 검사 → config toil 최소 |
| Defense in depth | 각 misbehavior를 보고하는 테스트/validator 확보 |
| Production probes | known bad / known good replayable / non-replayable 3묶음 |
| Canary | 0.1%→1%→10%→100% (24h 간격, 지리 변화) |

---

## 추가로 알면 좋은 내용

### Test Pyramid (테스트 피라미드)

고전적인 소프트웨어 테스팅 모델 — SRE 테스팅 전략과 정렬:

```
         / E2E \          ← 적게, 느리고 비쌈
        /  Integration \  ← 중간
       /     Unit       \ ← 많게, 빠르고 쌈
```

- Unit이 기반 (압도적 다수, 빠름)
- Integration이 중간
- E2E(System)이 정점 (적고 느리고 비쌈)
- SRE는 여기에 **release test**, **regression test**, **statistical/chaos test** 계층을 추가

### MTTR vs MTBF의 수학적 관계

```
가용성 ≈ MTTF / (MTTF + MTTR)

- MTBF ≈ MTTF (단일 실패 주기 가정 시)
- Zero MTTR 테스트는 MTTR을 사실상 0으로 → push 차단
- 결과적으로 사용자가 경험하는 MTBF 증가
```

### Hermetic Test (밀폐 테스트)

- 외부 의존(network, DB, 다른 서비스) 없이 격리된 환경에서 실행
- 결정론적(deterministic) 결과 보장 — flaky test의 주요 원인(외부 상태) 제거
- SRE 책에서 강조하는 "simple test"의 핵심 속성 — interactive 피드백 가능하게 함

### Flaky Test 처리

flaky test(같은 코드에서 간헐적 실패)는 신뢰성 신호를 약화:
- 통계적 추론으로 실제 pass 확률과 flakiness 정도 추정
- random seed 로깅으로 재현 시도
- flaky test는 "비신호"로 간주 — CI에서 quarantine 또는 상태 표시
- 근본 원인(주로 race condition, 시간 의존, 외부 상태) 수정 우선

### Chaos Engineering과의 관계

Chaos Monkey(Netflix), Jepsen(Aphyr)은 SRE 책에서 "statistical test"로 분류:
- 정해진 시나리오가 아닌 무작위 결함 주입
- 분산 시스템의 숨겨진 결함 발견
- SRE의 "통계적 테스트" 카테고리가 오늘날 **Chaos Engineering** 분야로 발전
- GameDays(Chapter 13/14)와 연결 — 통제된 환경에서 chaos 수행

### Test Coverage 목표 (경험적)

| 시스템 유형 | 권장 coverage |
|-------------|--------------|
| Life-critical / Revenue-critical | 매우 높음 (>90%) |
| 핵심 인프라 | 높음 |
| 일반 서비스 | 중간 |
| 비프로덕션 스크립트 | 낮음 허용 |

> "모든 소프트웨어가 동등하게 생성되지 않는다" — 수명과 중요도에 따라 차등 적용.

### Release Test 자동화

- Push gate: release test 통과 전 프로덕션 도달 차단
- Break-glass는 예외 — but noisy(버그 자동 filing)
- Canary: 0.1% → 100% 단계적 (24h 간격)
- SRE의 "Zero MTTR" 달성의 핵심 메커니즘

### Continuous Integration (CI)과의 정렬

SRE의 testing 인프라는 현대 CI/CD pipeline과 동일:
- Source control → Continuous build → Test (unit/integration) → Release test → Canary → Production
- Bazel의 의존성 그래프 → 변경 영향만 테스트 = 빠른 CI
- "latest version은 작동해야" = trunk-based development의 전제

### 다른 장과의 연결

| 연결 | 내용 |
|------|------|
| Chapter 4 (SLO) | 테스트 통과율 = 신뢰성 예측의 입력; SLO 위반 시 테스트 강화 |
| Chapter 6 (Monitoring) | testing(known) + monitoring(unknown) = 위험 커버 |
| Chapter 8 (Release Engineering) | release test, canary, progressive rollout |
| Chapter 12 (Troubleshooting) | bug → test 변환으로 재발 방지 |
| Chapter 15 (Postmortem) | action item으로 regression test 추가 |