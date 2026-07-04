# Chapter 15: Postmortem Culture — Learning from Failure

Written by John Lunney and Sue Lueder

> "The cost of failure is education." — Devin Carraway

---

## 들어가며

SRE는 대규모·복잡·분산 시스템을 다룬다. 서비스에 새 기능을 지속 추가하고 새 시스템을 도입한다. 이 규모와 변경 속도에서 **인시던트와 outage는 불가피**하다.

인시던트 발생 시 underlying issue를 수정하고 서비스를 정상 운영으로 복원한다. 하지만 이런 인시던트로부터 학습하는 **공식화된 프로세스**가 없으면, 동일한 인시던트가 무한 반복될 수 있다. 방치되면 인시던트는 복잡해지거나 cascade하여 시스템과 운영자를 압도하고 궁극적으로 사용자에게 영향을 준다.

**Postmortem은 SRE의 필수 도구**다.

Postmortem은 인시던트, 영향, 완화·해결 조치, root cause, 재발 방지 follow-up action을 기록한 **서면 문서**다.

---

## Google's Postmortem Philosophy

### Primary Goals

1. 인시던트 **문서화**
2. 모든 기여 root cause를 **충분히 이해**
3. **효과적인 예방 조치**로 재발 가능성·영향 감소

### Postmortem Triggers (작성 기준)

사전에 기준을 정의해야 한다 (인시던트 발생 후가 아니라 전에):

| 트리거 | 예시 |
|--------|------|
| 사용자에게 보이는 downtime/degradation | 특정 threshold 초과 |
| 데이터 손실 | 종류·규모 무관 |
| On-call 엔지니어 개입 | release rollback, traffic rerouting 등 |
| 해결 시간 | 특정 threshold 초과 |
| 모니터링 실패 | 수동 발견을 의미 |

이 외에도 이해관계자가 postmortem을 요청할 수 있다.

### Blameless Postmortem (무책임 문화)

SRE 문화의 핵심 원칙. 진정한 blameless postmortem은:

- 개인·팀의 "잘못된" 행동을 고발하지 않음
- 인시던트에 관여한 모든 사람이 **좋은 의도**로, 가진 정보로 **올바른 행동**을 했다고 가정
- **사람을 "고치는" 것이 아니라 시스템·프로세스를 고친다**

Blameless culture는 **의료·항공** 산업에서 기원 — 실수가 치명적일 수 있는 환경에서 모든 "실수"를 시스템 강화 기회로 본다.

손가락질·처벌 문화가 지배하면 사람들은 처벌 두려움으로 이슈를 숨긴다.

#### Blame vs Blameless 예시

**Blame (손가락질)**
> "복잡한 백엔드 전체를 다시 써야 해! 3분기 동안 매주 깨지고 있잖아. 한 번 더 페이지 오면 내가 직접 다시 쓸 거야…"

**Blameless (건설적)**
> "백엔드 전체 rewrite action item이 이런 페이지를 막을 수 있고, 이 버전의 maintenance manual이 길고 완전히 숙련되기 어렵다. 미래 on-caller들이 감사할 것이다."

### Best Practice: Avoid Blame and Keep It Constructive

- Blameless postmortem은 작성이 어렵다 — 형식이 인시던트를 유발한 행동을 명확히 식별하기 때문
- Blame 제거는 사람들이 **두려움 없이 이슈를 escalate**할 수 있는 자신감을 준다
- 특정 사람·팀의 빈번한 postmortem을 **낙인찍지 말 것** — 그렇지 않으면 이슈가 숨겨진다

---

## Collaborate and Share Knowledge

Postmortem 워크플로우는 모든 단계에서 **협업·지식 공유**를 포함한다.

### 도구 요구사항

| 기능 | 이유 |
|------|------|
| Real-time collaboration | 초기 postmortem 작성 시 빠른 데이터·아이디어 수집 |
| Open commenting/annotation | 크라우드소싱 솔루션, 커버리지 향상 |
| Email notifications | 협업자·추가 입력자 loop-in |

### Review Process

1. 팀 내부에서 첫 draft 공유
2. Senior engineer 그룹이 완성도 평가:
   - 핵심 인시던트 데이터 수집됐는가?
   - 영향 평가 완전한가?
   - Root cause 충분히 깊은가?
   - Action plan 적절하고 bug fix 우선순위 맞는가?
   - 관련 이해관계자에게 outcome 공유됐는가?
3. 더 넓은 engineering 팀·mailing list에 공유
4. **가능한 한 넓은 청중**에게 지식·교훈 전달

### Best Practice: No Postmortem Left Unreviewed

검토되지 않은 postmortem은 존재하지 않은 것과 같다. 정기 review session으로:
- 진행 중인 논의·코멘트 종료
- 아이디어 캡처
- 최종 상태 확정
- 팀/조직 repository에 추가

---

## Introducing a Postmortem Culture

도입은 말보다 어렵다 — 지속적 육성·강화 필요.

### Senior Management 참여

- Senior management가 review·collaboration에 적극 참여
- Blameless postmortem은 이상적으로 **엔지니어 자기 동기**의 산물
- SRE가 시스템 인프라에서 배운 것을 전파하는 활동을 자발적으로 생성

### 문화 확산 활동 예시

| 활동 | 설명 |
|------|------|
| **Postmortem of the Month** | 월간 뉴스레터에 흥미롭고 잘 쓰인 postmortem 공유 |
| **Google+ postmortem group** | 내부·외부 postmortem, best practice, 논평 공유 |
| **Postmortem reading clubs** | 팀이 정기적으로 postmortem 토론 (몇 달~몇 년 전 것도) |
| **Wheel of Misfortune** | 신규 SRE가 이전 postmortem을 role-play 재현 (원래 IC 참석) |

### 도입 전략

1. **Trial period**: 몇 개의 완성된 성공적 postmortem으로 가치 증명 + 기준 식별
2. **Reward**: 효과적인 postmortem 작성을 공개·성과 관리에서 보상
3. **Leadership**: Larry Page도 postmortem의 높은 가치를 언급

### Best Practice: Visibly Reward People for Doing the Right Thing

TGIF(주간 all-hands)에서 "The Art of the Postmortem" 발표 사례:
- SRE가 release push → 4분 outage → 즉시 rollback으로 대규모 outage 방지
- **Peer bonus 2개** + 수천 명 앞에서 박수 (founder 포함)
- 빠르고 침착한 대응이 인정받는 문화

### Best Practice: Ask for Feedback on Postmortem Effectiveness

정기 설문:
- 문화가 업무를 지원하는가?
- Postmortem 작성이 toil이 너무 많은가?
- 다른 팀에 추천할 best practice는?
- 어떤 도구가 필요한가?

---

## Conclusion and Ongoing Improvements

Postmortem 문화에 대한 지속 투자 덕분에 Google은 **더 적은 outage**와 **더 나은 사용자 경험**을 유지한다.

**"Postmortems at Google" working group**:
- Postmortem template 조율
- 인시던트 도구 데이터로 postmortem 생성 자동화
- Postmortem에서 데이터 추출 자동화 → trend analysis
- YouTube, Gmail, Cloud, Maps 등 다양한 제품 간 best practice 협업

향후 작업:
- Template에 metadata 필드 추가 (자동 분석 용이)
- Machine learning으로 약점 예측, 실시간 인시던트 조사, 중복 인시던트 감소

---

## 핵심 정리

| 개념 | 핵심 |
|------|------|
| Postmortem 목적 | 문서화 + root cause 이해 + 예방 조치 |
| Blameless | 사람이 아닌 시스템·프로세스 수정 |
| 트리거 | 사전 정의 (downtime, 데이터 손실, on-call 개입, 해결 시간, 모니터링 실패) |
| Review | 검토 없는 postmortem = 없는 것 |
| 문화 확산 | reading club, Wheel of Misfortune, 월간 공유, 리더십 참여 |
| 보상 | 빠른 대응·좋은 postmortem을 공개적으로 인정 |

---

## 추가로 알면 좋은 내용

### Postmortem 템플릿 구조

Google/Etsy/많은 조직이 사용하는 표준 섹션:

```markdown
# Postmortem: [Incident Title]
**Date**: YYYY-MM-DD
**Duration**: X hours Y minutes
**Severity**: SEV1/SEV2/...
**Authors**: [names]

## Summary
(2-3문장 요약)

## Impact
- Users affected: N
- Revenue impact: $X (if applicable)
- SLO breach: yes/no

## Timeline (all times UTC)
| Time | Event |
|------|-------|
| T+0  | Alert fired |
| T+5m | IC declared |

## Root Cause
(기술적 root cause + contributing factors)

## What Went Well
## What Went Wrong

## Action Items
| Action | Owner | Priority | Due Date |
|--------|-------|----------|----------|
| ...    | ...   | P0       | ...      |

## Lessons Learned
```

### Five Whys (5 Whys)

Root cause 분석의 고전적 기법 (Toyota Production System):

1. 왜 서비스가 다운됐나? → DB connection pool 고갈
2. 왜 pool이 고갈됐나? → slow query가 connection을 오래 점유
3. 왜 slow query가 있었나? → 인덱스 누락
4. 왜 인덱스가 없었나? → schema 변경 시 review 누락
5. 왜 review가 누락됐나? → schema 변경에 대한 자동 lint 없음

**Action item**: schema 변경 자동 lint 도입 (근본 원인 해결)

### Etsy의 Morgue

Etsy가 오픈소스로 공개한 postmortem 관리 도구:
- GitHub: `etsy/morgue`
- Postmortem 저장·검색·태깅
- Action item 추적
- Google의 내부 repository와 유사한 역할

### Blameless의 실천적 의미

"Blameless"가 "책임 없음"이 아니다:

| Blameful | Blameless |
|----------|-----------|
| "John이 잘못된 설정을 push했다" | "설정 변경에 canary가 없었다" |
| "팀 X가 테스트를 안 했다" | "테스트 커버리지 기준이 없었다" |
| 개인 처벌 | 시스템 개선 action item |

**Accountability는 유지** — action item owner는 명확. 다만 **개인 비난이 아닌 시스템 개선**에 초점.

### Postmortem vs RCA vs After Action Review (AAR)

| 방법 | 출처 | 초점 |
|------|------|------|
| Postmortem | IT/SRE | 기술적 root cause + action items |
| RCA (Root Cause Analysis) | 제조/품질 | 체계적 원인 분석 (5 Whys, Fishbone) |
| AAR (After Action Review) | 미군 | "What was supposed to happen vs what happened" |

SRE postmortem은 이 셋의 요소를 결합한다.

### Action Item 추적의 중요성

Postmortem의 가장 흔한 실패: **action item이 추적되지 않음**.

- Action item을 bug tracker에 등록 (Jira, Linear)
- Postmortem review에서 미완료 item 재논의
- **같은 root cause로 재발**하면 이전 postmortem의 action item 미이행을 확인

### "Near Miss" Postmortem

실제 outage 없이 **아슬아슬하게 피한 사건**도 postmortem 대상:
- Canary가 잡아낸 버그
- Rollback이 성공한 배포
- 모니터링이 없었다면 outage였을 이슈

Near miss postmortem은 **비용 대비 학습 가치**가 매우 높다.

### Postmortem 피해야 할 안티패턴

1. **너무 늦게 작성** — 기억이 희미해짐 (48시간 이내 목표)
2. **너무 길게** — 핵심이 묻힘 (5~10페이지 이내)
3. **Action item 없음** — "앞으로 조심하자"로 끝
4. **Review 없이 공유** — 품질 미검증
5. **같은 실수 반복** — action item 미추적

### SRE Book Appendix D: Example Postmortem

이 책의 Appendix D에 "Shakespeare Sonnet++ Postmortem (incident #465)" 예시가 있다. 실제 Google postmortem 형식의 참고 자료.