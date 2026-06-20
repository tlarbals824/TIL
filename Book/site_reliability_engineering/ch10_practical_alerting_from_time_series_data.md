# Chapter 10: Practical Alerting from Time-Series Data

Written by Jamie Wilkinson / Edited by Kavita Guliani

> "May the queries flow, and the pager stay silent." — Traditional SRE blessing

---

## 들어가며

모니터링은 Production Needs Hierarchy의 최하위 계층이자 안정적인 서비스 운영의 기본이다. 모니터링은 서비스 소유자가 서비스 변경의 영향을 합리적으로 판단하고, 인시던트 대응에 과학적 방법을 적용하며, 비즈니스 목표와의 정렬을 측정할 수 있게 한다.

대규모 시스템 모니터링의 도전:
- 분석해야 할 컴포넌트의 sheer number
- 엔지니어의 유지보수 부담을 합리적으로 낮게 유지해야 함

Google은 단순한 메트릭(평균 응답 시간)뿐 아니라, 해당 지역의 모든 웹 서버에 걸친 응답 시간 분포를 이해하여 latency tail의 기여 요인을 파악한다.

규모에서 단일 머신 장애에 대한 알림은 노이즈가 너무 커서 actionable하지 않다. 대신 의존 시스템의 장애에 robust한 시스템을 구축한다. 많은 개별 컴포넌트 대신 신호를 집계하고 아웃라이어를 prune한다.

---

## The Rise of Borgmon

2003년 Borg(작업 스케줄링 인프라) 생성 직후, 이를 보완하기 위한 모니터링 시스템 Borgmon이 만들어졌다.

Borgmon은 커스텀 스크립트(응답 확인 + 알림, 트렌드 시각화와 완전 분리) 모델에서 벗어나, time-series 수집을 모니터링 시스템의 1급 역할로 만들고, 스크립트를 time-series를 조작하여 차트와 알림을 생성하는 풍부한 언어로 대체했다.

**핵심 특징:**
- 공통 데이터 노출 형식(varz)에 의존 → subprocess 실행 비용과 네트워크 연결 설정 비용 절감
- white-box monitoring (Monitoring Distributed Systems 참조)
- 데이터는 차트 렌더링과 알림 생성(단순 산술)에 모두 사용
- 수집이 단명 프로세스가 아니므로, 수집된 데이터의 history를 알림 계산에도 사용

이 기능들은 Monitoring Distributed Systems에서 설명한 단순성 목표 달성에 도움을 준다. 오버헤드를 낮게 유지하여 서비스를 운영하는 사람들이 지속적인 변화에 민첩하게 대응할 수 있게 한다.

---

## Instrumentation of Applications

/varz HTTP 핸들러는 일반 텍스트로 모든 exported 변수를 한 줄에 키와 값(space-separated)으로 나열한다.

map-valued 변수 확장으로 라벨을 정의하고 값 테이블이나 히스토그램을 export할 수 있다.

예:
```
http_responses map:code 200:25 404:0 500:12
```

프로그램에 메트릭 추가는 필요한 코드 위치에서 단일 선언만 하면 된다.

이 스키마리스 텍스트 인터페이스는 새로운 계측 추가의 장벽을 매우 낮추지만, 진행 중인 유지보수와 trade-off가 있다. 변수 정의와 Borgmon 규칙에서의 사용이 분리되어 있어 주의 깊은 변경 관리가 필요하다.

---

## Exporting Variables & Collection

각 주요 언어는 Google 바이너리에 기본으로 내장된 HTTP 서버와 자동 등록되는 exported variable 인터페이스를 구현한다.

Borgmon 인스턴스는 다양한 name resolution 방법(BNS 등)으로 대상 목록을 구성한다. 대상 목록은 동적이며, service discovery를 사용하면 유지 비용을 줄이고 모니터링을 스케일할 수 있다.

정해진 간격으로 각 대상의 /varz URI를 fetch하여 값을 메모리에 저장. 각 대상의 수집을 interval 전체에 분산하여 lockstep을 피한다.

Borgmon은 각 대상에 대해 "synthetic" 변수도 기록:
- 이름이 host:port로 resolve되었는지
- 대상이 수집에 응답했는지
- health check 응답 여부
- 수집 완료 시간

이 synthetic 변수는 모니터링 대상이 unavailable한지 감지하는 규칙 작성에 유용하다.

---

## Storage in the Time-Series Arena

서비스는 일반적으로 많은 바이너리, 많은 task, 많은 머신, 많은 cluster에서 실행된다. Borgmon은 이 모든 데이터를 유연한 쿼리와 슬라이싱이 가능하도록 정리해야 한다.

Borgmon은 모든 데이터를 메모리 DB(정기적으로 디스크에 checkpoint)에 저장. 데이터 포인트는 (timestamp, value) 형태이며, time-series(고유 labelset으로 이름 지어짐)라는 연대순 리스트에 저장된다.

time-series는 개념적으로 시간에 따라 진행되는 1차원 숫자 행렬이며, 라벨 순열을 추가하면 다차원이 된다.

time-series arena: 고정 크기 메모리 블록. 가비지 컬렉터가 가장 오래된 항목을 만료. horizon은 arena에서 가장 최근~가장 오래된 항목 사이의 시간 간격 (쿼리 가능한 데이터 양).

단일 데이터 포인트 메모리 요구량 ≈ 24 bytes. 예: 1분 간격으로 12시간 동안 100만 unique time-series ≈ 17GB 미만 RAM.

주기적으로 in-memory 상태를 Time-Series Database (TSDB)라는 외부 시스템에 아카이브. Borgmon은 오래된 데이터에 대해 TSDB를 쿼리할 수 있다 (느리지만 더 크고 저렴).

---

## Labels and Vectors

time-series는 숫자와 타임스탬프 시퀀스로 저장되며, 이를 vectors라고 부른다. 벡터는 arena의 다차원 데이터 매트릭스의 슬라이스/크로스 섹션이다.

time-series의 이름은 labelset (key=value 쌍 집합). 중요한 라벨:
- var: 변수 이름
- job: 모니터링 대상 서버 타입 이름
- service: 사용자에게 서비스를 제공하는 job의 느슨한 컬렉션
- zone: Borgmon이 수집을 수행한 위치(보통 datacenter)

예: {var=http_requests,job=webserver,instance=host0:80,service=web,zone=us-west}

쿼리는 모든 라벨을 지정할 필요 없으며, 검색은 일치하는 모든 time-series를 vector로 반환한다.

기간 지정: {var=...,}[10m] → 지난 10분 history 반환.

---

## Rule Evaluation

Borgmon은 본질적으로 programmable calculator (일부 syntactic sugar로 알림 생성). 규칙(Borgmon rules)은 다른 time-series로부터 time-series를 계산하는 간단한 대수 표현식으로 구성.

규칙은 parallel threadpool에서 실행되지만 이전에 정의된 규칙을 입력으로 사용할 때는 순서에 의존. 벡터 크기가 규칙의 전체 runtime을 결정.

**집계(Aggregation)**는 분산 환경에서 규칙 평가의 초석. job의 모든 task로부터 time-series 합을 구해 job 전체로 취급.

예: datacenter 내 job의 queries-per-second 총 rate = 모든 query 카운터의 rate of change 합.

rate() 함수는 포함된 표현식의 총 delta를 가장 최근과 가장 오래된 값 사이의 시간 간격으로 나눈 값을 반환.

예제 규칙:
``` 
{var=task:http_requests:rate10m,job=webserver} =
  rate({var=http_requests,job=webserver}[10m]);

{var=dc:http_requests:rate10m,job=webserver} =
  sum without instance({var=task:http_requests:rate10m,job=webserver})
```

Google 관례: 계산된 변수 이름은 집계 수준, 변수 이름, 수행한 연산을 colon-separated triplet으로 포함 (예: task HTTP requests 10-minute rate).

---

## Alerting

알림 규칙 평가 결과는 true(알림 트리거) 또는 false. "flap"을 방지하기 위해 규칙은 알림이 전송되기 전에 true여야 하는 최소 duration을 허용 (보통 최소 2 rule evaluation cycles).

예:
```
{var=dc:http_errors:ratio_rate10m,job=webserver} > 0.01
  and {var=dc:http_errors:rate10m,job=webserver} > 1
  for 2m
  => ErrorRatioTooHigh
     details "webserver error ratio at %trigger_value%"
     labels { severity=page };
```

Alertmanager가 Alert RPC를 받아 알림을 올바른 대상(on-call, ticket queue 등)으로 라우팅. dedup, fan-in/out 지원.

---

## Sharding the Monitoring Topology & Black-Box Monitoring

Borgmon 간 streaming protocol으로 time-series 데이터를 전송. 일반적인 배포: 각 datacenter에 cluster-level Borgmon, global에 2개 이상.

sharding: scraping-only layer + DC aggregation layer. 상위 tier는 하위에서 원하는 데이터만 필터링.

**Black-Box Monitoring**:
- Borgmon은 white-box (내부 상태 검사).
- 사용자 관점 전체 그림을 위해 Prober 사용: 대상에 프로토콜 체크를 수행하고 성공/실패 보고.
- Prober는 응답 페이로드 검증, 값 추출, 히스토그램 export 가능.
- load balancer 앞/뒤 모두 프로빙하여 localized failure 감지와 억제.

---

## Maintaining the Configuration

Borgmon 설정은 규칙 정의와 모니터링 대상을 분리. 동일 규칙 세트를 많은 대상에 한 번에 적용 가능. 반복을 피하고 유지 비용 감소.

language templates (매크로 같은 시스템)로 규칙 라이브러리 구축 및 재사용.

Production Monitoring 팀은 continuous integration 서비스를 실행하여 규칙을 검증하고 프로덕션의 모든 Borgmon에 배포.

두 가지 주요 템플릿 클래스:
1. 코드 라이브러리에서 export된 변수의 schema를 성문화.
2. 단일 서버 task부터 global service footprint까지 데이터 집계를 관리하는 템플릿.

라벨은 여러 용도:
- 데이터 소스 (instance/job)
- 데이터 자체의 분해 (code 등)
- 서비스 전체 내 데이터의 locality/aggregation (zone, shard)

---

## 결론

Borgmon은 check-and-alert-per-target 모델을 mass variable collection + time-series 전반에 걸친 중앙 집중 rule evaluation으로 전환했다.

이 decoupling은 모니터링 대상 규모가 알림 규칙 규모와 독립적으로 스케일되게 한다. 규칙은 공통 time-series 형식 위에 추상화되어 유지 비용이 적다.

서비스 규모에 따라 유지보수 비용이 sublinear로 증가하도록 하는 것은 모니터링(및 모든 sustaining operations work)을 유지보수 가능하게 만드는 핵심이다.

Borgmon은 Google 내부지만, time-series 데이터를 알림 생성의 데이터 소스로 취급하는 아이디어는 이제 Prometheus, Riemann 등 오픈소스 도구를 통해 모두에게 접근 가능하다.

---

## 핵심 정리

| 개념 | 핵심 |
|------|------|
| White-box + Time-series | varz export, mass collection, history 활용 |
| Labels & Vectors | var/job/service/zone; rate, sum without instance |
| Rule Evaluation | rate(), aggregation, hierarchical computation |
| Alerting | threshold + duration, Alertmanager routing, avoid flapping |
| Sharding | per-DC + global Borgmon, streaming, filter at tiers |
| Black-box (Prober) | 사용자 관점 검증, response payload 검사 |
| Config 관리 | 규칙/대상 분리, templates, CI 검증 |
| 목표 | 유지보수 비용 sublinear 스케일 |

**중요 원칙**
- 증상을 알림 (원인 아님)
- 단순하고 재사용 가능한 템플릿
- 집계로 노이즈 줄이기
- white-box + black-box 결합으로 전체 그림 확보
