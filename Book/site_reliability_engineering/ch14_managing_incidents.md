# Chapter 14: Managing Incidents

Written by Andrew Stribblehill

> Effective incident management is key to limiting the disruption caused by an incident and restoring normal business operations as quickly as possible.

---

## 들어가며

효과적인 인시던트 관리는 장애로 인한 혼란을 제한하고 정상 운영을 최대한 빠르게 복원하는 핵심이다. 잠재적 인시던트에 대한 대응을 **사전에 게임화(game out)** 하지 않으면, 실제 상황에서 원칙 있는 인시던트 관리는 무너진다.

이 장은 통제 불능의 인시던트 초상화, 잘 관리된 접근법, 그리고 같은 인시던트가 잘 작동하는 인시던트 관리로 처리됐을 때의 시나리오를 대비한다.

---

## Unmanaged Incidents (관리되지 않은 인시던트)

Mary는 The Firm의 on-call 엔지니어. 목요일 오후 2시, pager가 폭발한다. Black-box 모니터링이 한 datacenter에서 트래픽 서빙이 완전히 중단됐다고 알린다.

- 몇 분 후 두 번째 datacenter 실패 (5개 중 2개)
- 세 번째 datacenter 실패 → 남은 datacenter가 과부하
- 서비스 전체 overload, 요청 처리 불가
- Mary는 로그를 읽다가 최근 업데이트된 모듈 오류로 판단, 롤백 시도 → 효과 없음
- 코드 작성자 Josephine에게 전화 (그녀의 시간대 새벽 3:30)
- 동료 Sabrina, Robin이 각자 터미널에서 "그냥 보는 중"
- 임원이 Mary의 상사에게 전화: "비즈니스 크리티컬 서비스의 완전 붕괴"를 왜 몰랐냐고 분노
- VP들이 ETA를 반복 질문, "어떻게 이런 일이?" — "Increase the page size!" 같은 엔지니어링과 무관한 조언
- Josephine이 Malcolm을 불렀고, Malcolm은 CPU affinity 최적화를 위해 **프로덕션에 변경 배포** → 서버 재시작 후 즉시 죽음

### The Anatomy of an Unmanaged Incident

모두 자기 역할을 했다. 그런데 왜 이렇게 망가졌는가?

| 함정 | 설명 |
|------|------|
| **Sharp Focus on the Technical Problem** | Mary는 기술적 문제 해결에 몰두해 큰 그림(완화 전략)을 생각할 여유가 없음 |
| **Poor Communication** | 동료들이 무엇을 하고 있는지 모름. 경영진 분노, 고객 좌절, 다른 엔지니어 미활용 |
| **Freelancing** | Malcolm이 Mary와 조율 없이 시스템 변경 → 나쁜 상황을 훨씬 악화 |

---

## Elements of Incident Management Process

Google의 인시던트 관리 시스템은 **Incident Command System (ICS)** 기반 — 명확성과 확장성으로 유명.

### Recursive Separation of Responsibilities (역할 분리)

모든 참여자가 자기 역할을 알고 남의 영역에 침범하지 않아야 한다. 역설적으로, **명확한 역할 분리가 더 많은 자율성**을 준다 — 동료를 의심할 필요가 없으니까.

부하가 과도하면 planning lead에게 추가 인력 요청 → 하위 인시던트 생성 또는 컴포넌트 위임.

### 핵심 역할

| 역할 | 책임 |
|------|------|
| **Incident Commander (IC)** | 인시던트의 high-level state 보유. task force 구성, 책임 위임. 위임하지 않은 모든 역할을 사실상 겸임. Ops의 roadblock 제거 |
| **Operational Work (Ops Lead)** | IC와 협력해 운영 도구로 대응. **인시던트 중 시스템을 수정하는 유일한 그룹** |
| **Communication** | 인시던트 대응의 공개적 얼굴. 주기적 업데이트(보통 이메일), incident document 정확·최신 유지 |
| **Planning** | Ops 지원. 버그 filing, 저녁 주문, 핸드오프, 시스템이 정상에서 벗어난 상태 추적(복구 후 revert용) |

### A Recognized Command Post (지휘소)

이해관계자가 IC와 상호작용할 위치를 알아야 한다.

- **War Room**: 물리적 중앙 집중 (많은 팀이 선호)
- **분산**: 각자 자리에서 IRC·이메일로 업데이트

Google은 **IRC**를 인시던트 대응에 크게 활용:
- 매우 안정적
- 이벤트 커뮤니케이션 로그로 활용 (postmortem 분석에 귀중)
- 알림·이벤트를 채널에 로깅하는 봇
- 지리적으로 분산된 팀 조율에 편리

### Live Incident State Document (라이브 인시던트 문서)

IC의 가장 중요한 책임: **살아있는 인시던트 문서** 유지.

- Wiki 또는 Google Docs (여러 사람 동시 편집)
- **인시던트 관리에 사용하는 소프트웨어를 고치려 하지 마라** — 잘 끝나지 않는다
- 템플릿 사용, 중요 정보를 상단에 배치
- postmortem·meta analysis를 위해 보존

### Clear, Live Handoff (명확한 핸드오프)

근무일 종료 시 IC 역할을 **명확히 인계**해야 한다.

- 전화/영상 통화로 새 IC에게 상황 전달
- **"You're now the incident commander, okay?"** — 명시적 확인 받을 때까지 통화 종료하지 않음
- 인시던트 참여자 전체에 핸드오프 공지

---

## A Managed Incident (관리된 인시던트)

같은 시나리오, 인시던트 관리 프레임워크 적용:

1. Mary가 pager 수신, 첫 datacenter 조사 시작
2. 두 번째 datacenter 실패 → 빠르게 성장하는 이슈이므로 **Sabrina에게 "Can you take command?"**
3. Sabrina가 이메일로 사전 정의된 mailing list에 상황 공유
4. 세 번째 alert → IRC에서 확인, 이메일 스레드 업데이트 (VP는 high-level status만)
5. Sabrina가 external communications 담당자에게 사용자 메시지 초안 요청
6. Josephine on-call 연락 — Mary 승인 후 loop-in
7. Robin 자원 → Sabrina가 **Mary에게 위임된 작업 우선, Mary에게 모든 추가 행동 보고** 리마인드
8. Mary의 롤백 시도 실패 → Robin이 IRC 업데이트, Sabrina가 문서에 반영
9. 5시: Sabrina가 교대 인력 확보, 5:45 전화 회의, 6시 sister office에 핸드오프
10. 다음 날 Mary 출근 → 대서양 동료가 bug 수정, 인시던트 종료, postmortem 시작 완료

---

## When to Declare an Incident

**일찍 선언하는 것이 낫다** — 간단한 수정으로 끝나면 닫으면 된다. 몇 시간 후에야 프레임워크를 돌리는 것보다 훨씬 낫다.

Google 팀의 선언 기준 (하나라도 해당하면 인시던트):

1. **두 번째 팀의 개입**이 필요한가?
2. **고객에게 outage가 보이는가?**
3. **1시간 집중 분석 후에도** 해결되지 않았는가?

### Practice (연습)

인시던트 관리 능력은 사용하지 않으면 빠르게 퇴화한다.

- **다른 운영 변경**에도 프레임워크 적용 (timezone/팀 spanning 변경)
- **재해 복구 테스트**에 인시던트 관리 포함
- **이미 해결된 on-call 이슈를 role-play** — 다른 location 동료가 해결한 사례 재현

---

## In Summary

사전에 인시던트 관리 전략을 수립하고, 확장 가능하게 구조화하며, 정기적으로 사용하면 **MTTR 감소**와 **덜 스트레스받는 비상 대응**이 가능하다.

### Best Practices for Incident Management

| 원칙 | 설명 |
|------|------|
| **Prioritize** | bleeding stop, 서비스 복원, root-cause를 위한 증거 보존 |
| **Prepare** | 사전에 절차 개발·문서화 (참여자와 협의) |
| **Trust** | 할당된 역할 내에서 참여자에게 완전한 자율성 |
| **Introspect** | 감정 상태 주의. 당황·압도되면 더 많은 지원 요청 |
| **Consider alternatives** | 주기적으로 옵션 재평가, 다른 접근 필요 여부 판단 |
| **Practice** | 프로세스를 일상적으로 사용해 자연스럽게 |
| **Change it around** | 매번 다른 역할 경험. 모든 팀원이 각 역할에 익숙해지도록 |

---

## 핵심 정리

| 관리 안 됨 | 관리됨 |
|-----------|--------|
| 기술 문제에만 집중 | IC가 전체 조율 |
| 커뮤니케이션 부재 | Communication 역할, IRC, 이메일 |
| Freelancing (무단 변경) | Ops만 시스템 수정, 위임·보고 체계 |
| 핸드오프 없음 | 명시적 IC 인계 |
| VP 직접 개입 | high-level status만 전달 |

**ICS 역할 4개**: Incident Commander, Ops, Communication, Planning

**인시던트 선언 조건**: 2팀 개입 / 고객 영향 / 1시간 미해결

---

## 추가로 알면 좋은 내용

### Incident Command System (ICS) 상세

ICS는 1970년대 미국 소방계에서 시작되어 FEMA가 표준화했다. 핵심 개념:

- **Span of control**: 한 사람이 직접 관리하는 하위 인원 수 제한 (보통 3~7명)
- **Unity of command**: 각 사람은 하나의 상급자에게만 보고
- **Common terminology**: 역할·상태에 대한 표준 용어
- **Modular organization**: 인시던트 규모에 따라 조직 확장/축소

IT에서의 적용: PagerDuty, Atlassian, Google SRE 모두 ICS에서 파생.

### Severity Levels (심각도 등급)

인시던트 선언 후 **심각도 분류**가 필요하다. 일반적 등급:

| 등급 | 기준 예시 |
|------|-----------|
| SEV1 / P0 | 전체 서비스 다운, 데이터 손실 |
| SEV2 / P1 | 주요 기능 장애, 다수 사용자 영향 |
| SEV3 / P2 | 부분 장애, workaround 존재 |
| SEV4 / P3 | 경미한 이슈, 모니터링만 |

심각도에 따라 에스컬레이션 범위, 커뮤니케이션 빈도, 경영진 참여 수준이 달라진다.

### Incident Timeline (타임라인)

잘 관리된 인시던트는 **타임라인**이 자동으로 기록된다:

```
T+0    Alert fired
T+2m   IC declared, roles assigned
T+5m   First mitigation attempted (rollback)
T+15m  Customer comms sent
T+30m  Root cause identified
T+45m  Fix deployed
T+60m  Incident resolved, monitoring
```

이 타임라인은 postmortem의 핵심 입력이 된다.

### "Freelancing" 방지 — Change Freeze

인시던트 중 **변경 동결(change freeze)** 정책:
- Ops Lead만 시스템 변경 가능
- 모든 변경은 IC 승인 + incident document 기록
- "도움이 될 것 같아서" 하는 변경은 금지 (Malcolm 사례)

### Customer Communication

Communication 역할의 확장:
- **Status page** 업데이트 (Statuspage, Instatus)
- **고객 지원팀** 브리핑
- **내부 이해관계자** (VP, PM) — 기술 디테일이 아닌 impact/ETA

"고객에게 보이지 않는" 인시던트도 내부 커뮤니케이션은 필수.

### War Room vs Virtual

| 방식 | 장점 | 단점 |
|------|------|------|
| Physical War Room | 빠른 대면 조율, 집중 | 원격 팀 배제 |
| Virtual (IRC/Slack) | 분산 팀, 로그 자동 | 컨텍스트 스위칭, 산만 |

현대 조직은 **하이브리드**가 일반적: IC + Ops는 War Room(또는 Zoom), Communication은 Slack/이메일.

### 인시던트 관리 연습 방법

1. **Tabletop exercise**: 실제 시스템 건드리지 않고 시나리오 토론
2. **Game day**: 실제 장애 주입 + 인시던트 프로세스 실행
3. **Role rotation**: 매 분기 다른 역할 경험
4. **Post-incident review**: 인시던트 관리 자체를 평가 (기술 문제와 별도)

### 참고: ITIL vs SRE Incident Management

**ITIL** (IT Infrastructure Library)도 인시던트 관리를 다루지만 SRE 접근과 차이:

| ITIL | SRE/ICS |
|------|---------|
| Service desk 중심 | Engineering 중심 |
| SLA 복원 우선 | 사용자 영향 최소화 + 학습 |
| Change Advisory Board | Ops Lead + IC 승인 |
| Problem Management (별도) | Postmortem이 problem management 겸함 |