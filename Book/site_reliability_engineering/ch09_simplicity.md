# Chapter 9: Simplicity

Written by Max Luebbe / Edited by Tim Harvey

---

## 들어가며

"The price of reliability is the pursuit of the utmost simplicity." — C.A.R. Hoare, Turing Award lecture

소프트웨어 시스템은 본질적으로 동적이고 불안정하다. 완벽하게 안정적인 시스템은 진공 상태에 존재해야 가능하다. 코드베이스를 변경하지 않으면 버그 도입을 멈출 수 있지만, 하드웨어나 라이브러리가 변경되면 문제가 생긴다. 사용자 기반을 고정하면 스케일 문제도 없다.

SRE의 핵심: "At the end of the day, our job is to keep agility and stability in balance in the system."

---

## System Stability Versus Agility

안정성을 위해 민첩성을 희생하는 것이 때로는 합리적이다. SRE는 신뢰성을 높이는 절차·도구를 만들면서도 개발자의 민첩성에 미치는 영향을 최소화해야 한다. 실제로 신뢰성 있는 프로세스는 개발 민첩성을 높인다: 빠르고 신뢰성 있는 프로덕션 롤아웃은 변경을 쉽게 관찰하게 하고, 버그 발견 시 빠른 수정이 가능하다.

**핵심 원칙:**
- 우발적 복잡성(accidental complexity)이 시스템에 도입되면 적극적으로 저지하라.
- 담당하는 시스템의 복잡성을 지속적으로 제거하라.

---

## The Virtue of Boring

소프트웨어에서 "boring"은 긍정적 속성이다. 프로그램이 자발적이고 흥미로울 필요는 없다. 스크립트를 정확히 따르고 예측 가능하게 비즈니스 목표를 달성해야 한다.

"Unlike a detective story, the lack of excitement, suspense, and puzzles is actually a desirable property of source code." — Robert Muth

Fred Brooks의 "No Silver Bullet": 필수 복잡성(essential complexity)과 우발적 복잡성(accidental complexity)을 구분하라. 우발적 복잡성은 엔지니어링 노력으로 제거 가능하다.

---

## I Won’t Give Up My Code!

엔지니어는 자신의 창작물에 감정적으로 집착한다. 대규모 소스 트리 정리 시 충돌이 흔하다. "나중에 필요할 수 있다", "주석 처리만 하자", "플래그로 게이트하자"는 모두 나쁜 제안이다.

소스 컨트롤은 변경을 쉽게 되돌릴 수 있지만, 수백 줄의 주석 코드는 산만함과 혼란을 초래하고, 절대 실행되지 않는 코드(플래그가 항상 비활성화된)는 시한폭탄이다 (Knight Capital 사례 참조).

24/7 가용성이 기대되는 웹 서비스에서, 새로 작성되는 모든 코드 라인은 일종의 부채(liability)다. SRE는 코드가 실제 비즈니스 목표를 추진하는지 면밀히 검토하고, 죽은 코드(dead code)를 정기적으로 제거하며, 모든 테스트 레벨에 블로트(bloat) 감지를 내장한다.

---

## The "Negative Lines of Code" Metric

"software bloat"은 시간이 지남에 따라 추가 기능의 지속적인 유입으로 소프트웨어가 느려지고 커지는 경향을 의미한다.

SRE 관점에서 부정적 측면:
- 프로젝트에 추가되거나 변경된 모든 코드 라인은 새로운 결함과 버그의 잠재력을 만든다.
- 작은 프로젝트는 이해하기 쉽고, 테스트하기 쉽고, 결함이 적다.

가장 만족스러운 코딩 중 일부는 더 이상 유용하지 않은 수천 줄의 코드를 삭제하는 것이었다.

---

## Minimal APIs

"perfection is finally attained not when there is no longer more to add, but when there is no longer anything to take away." — Antoine de Saint-Exupéry

API는 소프트웨어 시스템에서 단순성을 관리하는 핵심 표현이다. 소비자에게 제공하는 메서드와 인자가 적을수록 API는 이해하기 쉽고, 해당 메서드를 최대한 좋게 만드는 데 더 많은 노력을 쏟을 수 있다.

"less is more!" 작은 단순한 API는 보통 잘 이해된 문제의 특징이다.

---

## Modularity

API와 단일 바이너리를 넘어, 객체 지향 프로그래밍의 많은 규칙이 분산 시스템 설계에도 적용된다. 시스템 일부를 격리하여 변경할 수 있는 능력은 지원 가능한 시스템을 만드는 데 필수적이다.

- 바이너리 간, 또는 바이너리와 설정 간의 느슨한 결합(loose coupling)은 개발자 민첩성과 시스템 안정성을 동시에 촉진하는 단순성 패턴.
- 한 컴포넌트의 버그는 시스템의 나머지 부분과 독립적으로 수정·배포 가능.

API 변경도 마찬가지: 단일 API 변경은 개발자가 전체 시스템을 재빌드하게 만들고 새 버그 위험을 초래한다. API 버저닝을 통해 기존 버전에 의존하는 시스템을 안전하게 업그레이드할 수 있다.

"util" 또는 "misc" 바이너리를 프로덕션에 두는 것은 나쁜 관행이다. 잘 설계된 분산 시스템은 각자가 명확하고 잘 범위가 정해진 목적을 가진 협력자들로 구성된다.

데이터 포맷에도 적용: Google의 protocol buffers 설계 목표 중 하나는 backward/forward 호환 가능한 와이어 포맷을 만드는 것이었다.

---

## Release Simplicity

단순한 릴리즈가 복잡한 릴리즈보다 낫다. 단일 변경의 영향을 측정하고 이해하기 훨씬 쉽다. 100개의 관련 없는 변경을 동시에 릴리즈하면 성능이 나빠졌을 때 어떤 변경이 영향을 주었는지 파악하는 데 많은 노력과 계측이 필요하다.

작은 배치로 릴리즈하면 각 코드 변경을 큰 시스템에서 격리하여 이해할 수 있어 더 빠르고 자신 있게 진행 가능. 이는 머신러닝의 gradient descent와 유사: 한 번에 작은 단계로 최적해를 찾는다.

---

## A Simple Conclusion

이 장에서 반복된 주제: 소프트웨어 단순성은 신뢰성의 전제 조건이다. 각 작업 단계를 어떻게 단순화할지 고려하는 것은 게으름이 아니라, 실제로 달성하고자 하는 것이 무엇이며 가장 쉽게 하는 방법을 명확히 하는 것이다.

"no"라고 말할 때마다 혁신을 제한하는 것이 아니라, 산만함으로부터 환경을 깔끔하게 유지하여 혁신에 초점을 맞추고 진정한 엔지니어링이 진행되도록 한다.

---

## 핵심 정리

| 개념 | 핵심 |
|------|------|
| 신뢰성의 대가 | utmost simplicity 추구 |
| 우발적 복잡성 | 제거해야 할 대상; 필수 복잡성과 구분 |
| "Boring" 코드 | 예측 가능하고 스크립트를 따르는 것이 바람직 |
| Negative LOC | 죽은 코드 삭제는 긍정적; bloat 방지 |
| Minimal API | 적을수록 좋음; 잘 이해된 문제의 표시 |
| Modularity & Loose Coupling | 격리된 변경, API 버저닝, "util" 바이너리 피하기 |
| Release Simplicity | 작은 배치 릴리즈로 영향 파악 용이 |
| 결론 | 모든 "no"는 산만함 제거와 혁신 집중을 위한 것 |

**주요 인용**
> "The price of reliability is the pursuit of the utmost simplicity." — C.A.R. Hoare
> "Unlike a detective story, the lack of excitement, suspense, and puzzles is actually a desirable property of source code." — Robert Muth
