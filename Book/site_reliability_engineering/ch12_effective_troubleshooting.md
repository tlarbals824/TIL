# Chapter 12: Effective Troubleshooting

Written by Chris Jones

> "Be warned that being an expert is more than understanding how a system is supposed to work. Expertise is gained by investigating why a system doesn't work." — Brian Redman
> "Ways in which things go right are special cases of the ways in which things go wrong." — John Allspaw

---

## 들어가며

Troubleshooting은 분산 컴퓨팅 시스템을 운영하는 사람(특히 SRE)에게 핵심 기술이지만, 종종 어떤 사람은 타고나고 어떤 사람은 아닌 선천적 기술로 여겨진다. 그러나 troubleshooting은 배우고 가르칠 수 있다.

초보자는 두 가지 요소에 의존하기 때문에 어려움을 겪는다:
1. 일반적인 troubleshooting 방법 이해 (특정 시스템 지식 없이)
2. 시스템에 대한 탄탄한 지식

시스템 지식 없이 first principles만으로 조사할 수도 있지만, 보통 시스템이 어떻게 작동해야 하는지 아는 것보다 덜 효율적이고 효과적이다.

---

## Theory

Troubleshooting 프로세스는 hypothetico-deductive method의 적용으로 볼 수 있다: 시스템에 대한 관찰 세트와 시스템 동작에 대한 이론적 기반이 주어지면, 잠재적 원인을 가설로 세우고 이를 반복적으로 테스트한다.

이상적인 모델:
1. Problem report (무언가 잘못되었다는 보고)
2. 시스템 telemetry와 로그를 보고 현재 상태 파악
3. 시스템 설계/운영/실패 모드 지식과 결합하여 가능한 원인 식별
4. 가설 테스트 (관찰된 상태와 이론 비교, 또는 시스템을 제어적으로 변경하고 결과 관찰)
5. root cause 식별 → 시정 조치 → postmortem

---

## Common Pitfalls

비효율적인 troubleshooting 세션은 Triage, Examine, Diagnose 단계에서 문제가 많다 (대개 깊은 시스템 이해 부족 때문).

**피해야 할 일반적인 함정:**
- 관련 없거나 오해된 시스템 메트릭의 증상을 살펴보기 → wild goose chase
- 시스템, 입력, 환경을 변경하는 방법을 오해하여 가설을 안전하고 효과적으로 테스트하지 못함
- 터무니없이 있을 법하지 않은 이론을 세우거나, 과거 문제의 원인에 집착 ("한 번 일어났으니 다시 일어나고 있다")
- 실제로는 우연이거나 공유 원인과 상관된 spurious correlation을 추적

이 함정들을 피하려면 시스템을 배우고 분산 시스템에서 사용되는 일반적인 패턴에 익숙해져야 한다. 세 번째 함정은 논리적 오류로, "hoofbeats를 들으면 horses를 생각하라"(zebra가 아님)와 Occam's Razor를 기억하여 피할 수 있다.

또한 correlation ≠ causation임을 기억하라.

---

## In Practice

**Problem Report**
- 모든 문제는 문제 보고서로 시작 (자동 알림 또는 "시스템이 느리다").
- 좋은 보고서는 기대 동작, 실제 동작, 재현 방법을 알려야 함.
- Google에서는 모든 이슈에 대해 버그를 여는 것이 일반적 (이메일/IM으로 받은 경우에도). 이는 조사 및 수정 활동의 로그를 생성.
- 사람에게 직접 보고하는 것을 권장하지 않음: 추가 transcribe 단계, 낮은 품질 보고서, 소수 팀원에게 부하 집중.

**Triage**
- 문제 보고서를 받으면 다음으로 할 일을 파악.
- 심각도에 따라 대응: 소수 사용자 영향 vs 글로벌 전체 중단.
- 첫 본능(빠르게 root cause 찾기)을 억제하고, 상황에서 시스템을 최대한 잘 작동하게 만드는 것이 우선.
- "bleeding을 멈추는 것"이 첫 우선순위 (사용자에게 도움이 안 됨).
- 증거 보존(로그)도 병행.

"Novice pilots are taught that their first responsibility in an emergency is to fly the airplane; troubleshooting is secondary."

---

## Examine

각 컴포넌트가 무엇을 하고 있는지 검사하여 올바르게 동작하는지 이해해야 함.

**주요 도구:**
- Monitoring system (time-series, 그래프, 연산으로 동작 이해 및 상관관계 발견)
- Logging (각 operation과 시스템 상태에 대한 정보 export)
- Tracing (Dapper 등으로 전체 스택의 요청 추적)
- 현재 상태 노출 엔드포인트 (최근 RPC 샘플, histogram, 구성, 데이터 검사)

Text 로그는 실시간 reactive debugging에 유용. 구조화된 바이너리 형식 저장은 더 많은 정보로 사후 분석 가능.

다양한 verbosity level + 필요 시 동적 증가 기능이 강력. 통계적 샘플링(예: 1/1000)도 유용.

---

## Diagnose

**Simplify and reduce**
- 컴포넌트는 잘 정의된 인터페이스와 입력→출력 변환을 가짐.
- 각 단계에서 알려진 테스트 데이터를 주입하여 예상 출력인지 확인 (black-box testing).
- Divide and conquer: 스택 한쪽 끝에서 시작하여 반대쪽으로 체계적으로 진행.
- Bisection: 시스템을 반으로 나누고 한쪽의 통신 경로를 검사한 후 반복.

**Ask "what," "where," and "why"**
- 오작동 시스템은 여전히 무언가를 하려고 시도하고 있음 (원하는 것과 다를 뿐).
- 무엇을 하고 있는지, 왜 그렇게 하는지, 자원이 어디서 사용되는지/출력이 어디로 가는지 파악.

**What touched it last**
- 시스템에는 관성(inertia)이 있음. 최근 변경(구성 변경, 제공되는 부하 유형 변화)은 문제 파악의 생산적인 시작점.
- 잘 설계된 시스템은 모든 스택 계층에서 배포와 구성 변경을 추적하는 광범위한 프로덕션 로깅을 가짐.
- 성능/동작 변화와 시스템/환경의 다른 이벤트 상관.

---

## Test and Treat (Cure)

가능한 원인의 짧은 목록을 만들었으면, 실제 문제의 root가 되는 요인을 찾을 차례.

실험적 방법으로 가설을 rule in/out.

**고려사항:**
- 이상적인 테스트는 상호 배타적인 대안을 가져야 함 (한 그룹의 가설을 rule in, 다른 세트를 rule out).
- 명백한 것부터: 가능성 내림차순으로 테스트, 시스템에 대한 위험 고려.
- 실험은 교란 요인(confounding factors)으로 인해 오해의 소지가 있는 결과를 낼 수 있음.
- Active 테스트는 미래 테스트 결과에 side effect를 일으킬 수 있음 (예: verbose logging이 latency 문제를 악화).

Negative Results Are Magic: 기대한 효과가 나타나지 않은 실험 결과도 매우 가치 있음. 결론적이며, 다른 사람들이 유사한 실험을 반복할 필요를 없애고, 과거 설계의 함정을 피하게 함. 부정적 결과도 출판하라.

---

## Case Study (App Engine)

내부 고객이 latency, CPU 사용량, serving process 수의 극적인 증가를 보고. 트래픽 증가 없음. 토요일에 발생 (변경 없음).

조사:
- Dapper로 요청 추적.
- datastore suboptimal indexing 의심 → 하지만 static content 요청도 느려짐 (이론 반박).
- whitelist cache 문제 발견.
- 보안 스캐너가 수천 개의 whitelist 객체를 생성한 것이 원인.
- 버그 수정 + 객체 제거로 정상화.

---

## Making Troubleshooting Easier

- 각 컴포넌트에 잘 이해되고 관찰 가능한 인터페이스를 ground-up으로 설계.
- 시스템 전체에서 정보가 일관된 방식으로 사용 가능하게 (예: RPC 스팬 전반의 unique request identifier).
- 변경(코드/환경)을 단순화, 제어, 로깅하여 troubleshooting 필요성을 줄이고 쉽게 만듦.

---

## Conclusion

troubleshooting 과정을 초보자도 이해하고 효과적으로 수행할 수 있도록 명확하고 체계적으로 만드는 단계들을 살펴보았다. luck이나 경험에 의존하는 대신 체계적인 접근법을 채택하면 서비스의 회복 시간(MTTR)을 제한하고 사용자 경험을 개선할 수 있다.

---

## 핵심 정리

| 단계/개념 | 핵심 |
|-----------|------|
| Theory | hypothetico-deductive: 관찰 → 가설 → 테스트 → root cause |
| Triage 우선 | bleeding stop (시스템을 최대한 잘 작동하게), 그 다음 root cause |
| Examine | monitoring + structured logging + tracing + state exposure |
| Diagnose | simplify/reduce, divide & conquer / bisection, what/where/why, 최근 변경 확인 |
| Test & Treat | 가설 rule in/out, confounding factor 주의, side effect 고려 |
| Negative results | 가치 있음; 출판하여 다른 사람 돕기 |
| 함정 피하기 | 상관≠인과, wild goose chase, 과거 원인 집착, "horses not zebras" |
| Making easier | observable interfaces, consistent identifiers, 변경 제어/로깅 |

**주요 교훈**
- 시스템 지식 + 일반 프로세스의 결합이 가장 강력
- 문서화된 negative result와 postmortem은 산업 전체의 자산
- 체계적 접근 = 더 빠르고 예측 가능한 회복
