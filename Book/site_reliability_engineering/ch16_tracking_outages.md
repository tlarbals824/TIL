# Chapter 16: Tracking Outages

Written by Gabe Krabbe

> Improving reliability over time is only possible if you start from a known baseline and can track progress.

---

## 들어가며

시간에 따른 신뢰성 개선은 **알려진 baseline에서 시작**하고 **진행 상황을 추적**할 수 있을 때만 가능하다. Google의 outage tracker **"Outalator"**가 바로 이 역할을 한다.

Outalator는 모니터링 시스템이 보내는 **모든 alert를 수동으로 수신**하고, 이 데이터를 annotate, group, analyze할 수 있게 해주는 시스템이다.

---

## 왜 Outage Tracking이 필요한가?

과거 문제로부터 체계적으로 학습하는 것은 효과적인 서비스 관리에 필수다. Postmortem(Chapter 15)은 개별 outage에 대한 상세 정보를 제공하지만, **전체 그림의 일부**에 불과하다.

| Postmortem의 한계 | Outage Tracking이 보완하는 것 |
|-------------------|-------------------------------|
| 큰 영향의 인시던트만 작성 | 개별 영향은 작지만 빈번·광범위한 이슈 포착 |
| 단일 서비스/서비스 집합 개선 | 수평적(horizontal) 영향의 기회 식별 |
| 비용 대비 효과가 낮은 개선 누락 | 여러 이벤트에 걸친 작은 개선의 누적 효과 |

### 추적하면 좋은 질문들

- "이 팀이 on-call shift당 몇 개의 alert를 받는가?"
- "지난 분기 actionable/nonactionable alert 비율은?"
- "이 팀이 관리하는 서비스 중 toil을 가장 많이 만드는 것은?"

---

## Escalator

Google의 모든 SRE alert 알림은 **중앙 복제 시스템**을 공유한다. 이 시스템은 사람이 알림 수신을 확인(acknowledge)했는지 추적한다.

- 설정된 interval 내 ack 없으면 → 다음 대상으로 **escalation** (primary on-call → secondary)
- 초기 설계: on-call alias로 보내는 이메일의 **복사본을 수신**
- 기존 워크플로우에 쉽게 통합 (사용자 행동·모니터링 시스템 변경 불필요)

---

## Outalator

Escalator처럼 기존 인프라에 유용한 기능을 추가한 것처럼, Outalator는 개별 escalating notification이 아닌 **다음 추상화 계층: outage**를 다룬다.

### 핵심 기능

**Queue View**
- 여러 queue의 notification을 **시간순으로 인터리브**하여 한 화면에 표시
- 하나의 SRE 팀이 서로 다른 secondary escalation target을 가진 서비스들의 primary contact point인 경우 유용

**Annotation (주석)**
- 원본 notification 복사본 저장
- 인시던트에 주석 추가
- 이메일 reply도 자동 수신·저장
- 주석을 **"important"**로 표시 가능 → clutter 감소

**Grouping (그룹화)**
- 여러 escalating notification("alerts")을 하나의 entity("incident")로 결합
- 관련 alert, 무관한 auditable event, spurious monitoring failure 등 구분
- **"incidents per day" vs "alerts per day"** 별도 분석 가능

---

## Building Your Own Outalator

Slack, Hipchat, IRC 등 메시징 시스템은 내부 커뮤니케이션·상태 대시보드에 널리 사용된다. Outalator 같은 시스템을 **hook into**하기 좋은 곳이다.

### Aggregation (집계)

단일 이벤트가 여러 alert를 트리거하는 것은 흔하고, 종종 불가피하다:

- 네트워크 장애 → 모든 영향받는 팀 + NOC 각자 alert
- 단일 서비스 이슈도 여러 error condition으로 복수 alert

False positive vs false negative 트레이드오프에서 복수 alert는 대부분 불가피.

**여러 alert를 하나의 incident로 그룹화하는 능력이 핵심**:
- "이건 저 alert와 같은 것" 이메일은 하나의 alert에만 효과적
- 팀 간·장기간에 걸친 중복 alert 처리에는 비실용적

### Tagging (태깅)

모든 alerting event가 incident는 아니다. False positive, test event, 잘못된 대상 이메일 등.

Outalator는 이벤트를 구분하지 않지만, **범용 태깅**으로 notification에 metadata 추가 가능.

**태그 규칙:**
- 대부분 자유 형식 단일 "word"
- **콜론(`:`)은 semantic separator** → 계층적 namespace 자동 처리
- 제안 prefix: `cause`, `action` (팀별, 역사적 사용 기반)
- 예: `cause:network`, `cause:network:switch`, `cause:network:cable`
- `bug:76543` → bug tracker 링크 자동 생성
- `bogus` → false positive (널리 사용)
- `problem-went-away` → 별로 도움 안 되는 태그도 있음

**미리 정해진 목록을 피하고** 팀이 자체 선호·표준을 찾게 하면 더 유용한 도구와 데이터가 된다.

### Analysis (분석)

**Layer 1 — 기본 집계 통계**
- incidents per week/month/quarter
- alerts per incident

**Layer 2 — 비교**
- 팀/서비스 간 비교
- 시간에 따른 추세
- "이번 주 세 번째" — 예전엔 하루 5번이었는지, 한 달 5번이었는지 해석 가능

**Layer 3 — 의미 분석**
- 가장 많은 incident를 유발한 인프라 컴포넌트 식별
- 예: 여러 팀의 "stale data", "high latency" alert가 네트워크 혼잡→DB replication delay로 인한 것인지, SLO 내이지만 사용자 기대 미달인지
- **수평적 문제 식별** → 올바른 해결책 선택 (over-performing을 막기 위한 artificial failure 도입 등)

### Reporting and Communication

**Shift handoff**
- 0개 이상의 outalations 선택 → subject, tags, important annotations를 다음 on-call + cc list에 이메일

**Report mode**
- 주간 production review용
- important annotations를 메인 리스트에 inline 확장 → lowlights 빠른 overview

---

## Unexpected Benefits (예상치 못한 이점)

### Cross-Team Visibility

특정 alert flood가 다른 outage와 동시에 발생하는 것을 식별:
- 진단 속도 향상
- 다른 팀 부하 감소 (이미 incident임을 인정)

**비자명한 이점**: Bigtable 장애로 보이지만 Bigtable SRE 팀이 alert 받지 않았다면 → **수동으로 alert하는 것이 좋음**.

### Dummy Escalator Configurations

일부 팀이 **dummy escalator 설정** 구축:
- 사람은 notification을 받지 않음
- Outalator에 notification이 나타나 tag·annotate·review 가능

**용도:**
- Privileged/role account 사용 로깅·감사 (기술적 audit, 법적 audit 아님)
- Idempotent하지 않을 수 있는 주기적 job 실행 기록·자동 annotate (예: schema 변경 자동 적용)

---

## 핵심 정리

| 구성요소 | 역할 |
|----------|------|
| Escalator | Alert ack 추적, escalation |
| Outalator | Alert 수집, 그룹화, 태깅, 분석 |
| Aggregation | 복수 alert → 단일 incident |
| Tagging | `cause:`, `action:`, `bug:` 등 metadata |
| Analysis | 집계 → 비교 → 의미 분석 3계층 |
| Handoff | Shift 간 상태 전달 |

**Postmortem vs Outalator**
- Postmortem: 큰 영향, 깊은 분석, action items
- Outalator: 모든 alert, 빈도·패턴·추세, 수평적 인사이트

---

## 추가로 알면 좋은 내용

### Alert Fatigue와 Signal-to-Noise Ratio

Outalator가 해결하려는 근본 문제: **alert fatigue**.

| 지표 | 건강한 범위 (경험적) |
|------|---------------------|
| Alerts per on-call shift | < 2 actionable |
| Actionable ratio | > 50% |
| Pages per week (per person) | < 2 |

Chapter 11에서도 교대당 인시던트 ≤ 2를 권장한다. Outalator는 이 지표를 **측정**하는 도구다.

### SLO 기반 Alerting과의 관계

Chapter 4 (SLO)와 연결:
- SLO burn rate alert → Outalator에 기록
- "SLO 위반" 태그로 그룹화
- 분기별 SLO breach 횟수 추세 → error budget 정책 조정 근거

### Modern 대안 도구

| 도구 | Outalator 유사 기능 |
|------|---------------------|
| **PagerDuty** | Incident grouping, analytics, on-call metrics |
| **Datadog Incident Management** | Alert correlation, timeline, postmortem integration |
| **Grafana OnCall** | Alert grouping, escalation, shift handoff |
| **FireHydrant** | Incident timeline, retrospective automation |
| **Rootly** | Slack-native incident management + postmortem |

### Metrics to Track (추천 대시보드)

```
# Team-level
- MTTR (Mean Time To Recovery) trend
- Incidents per month
- Alerts per incident ratio
- Top 5 alert sources (by count)
- Top 5 tags (cause:*)

# Org-level
- Incidents by service/team
- Cross-team incident correlation
- Repeat incidents (same root cause tag)
- Actionable vs non-actionable alert ratio
- On-call load distribution (fairness)
```

### Toil Identification

Outalator 데이터로 **toil 식별** (Chapter 5):

- 같은 alert가 반복 → 자동화 후보
- `action:manual-restart` 태그 빈도 → 자동 restart 도입
- `cause:config-drift` 빈도 → configuration management 강화
- Non-actionable alert 비율 높음 → alert tuning 필요

### Incident Correlation (상관관계)

여러 팀의 alert가 동시에 발생할 때:

```
Team A: "high latency" (10:00)
Team B: "high latency" (10:01)
Team C: "DB connection errors" (10:02)
NOC:    "network link down" (10:00)
```

Outalator에서 이들을 **하나의 incident**로 그룹화 → Team A/B/C가 각자 디버깅하는 대신 NOC incident에 집중.

### Data-Driven Reliability Improvement Cycle

```
1. Outalator: alert/outage 데이터 수집
2. Analysis: 패턴·추세 식별
3. Postmortem: 큰 인시던트 심층 분석
4. Action items: 시스템·프로세스 개선
5. Measure: 다음 분기 Outalator 데이터로 개선 확인
6. Repeat
```

이 사이클이 SRE의 **지속적 신뢰성 개선**의 핵심 루프다.

### "Most Incidents Caused" Metric 주의

책에서 경고하는 함정:

- 가장 많은 incident를 유발한 컴포넌트 ≠ 가장 시급히 수정해야 할 것
- **과민한 모니터링** artifact일 수 있음
- **소수 misbehaving client**일 수 있음
- Incident 수 ≠ 수정 난이도 또는 영향 심각도

태그 + postmortem + 영향 평가를 **함께** 봐야 올바른 우선순위 결정 가능.