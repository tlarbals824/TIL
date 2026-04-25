# Chapter 2 - The Production Environment at Google, from the Viewpoint of an SRE

> Google 데이터센터는 일반 데이터센터와 매우 다릅니다. 이 장은 책 전반에 걸쳐 사용될 Google 인프라 용어를 정의합니다.

## 기본 용어 정의

Google에서는 일반적인 단어 사용과 다른 용어 정의를 사용합니다.

| 용어 | 의미 |
|------|------|
| **Machine** | 하드웨어 (또는 VM) |
| **Server** | 서비스를 구현하는 소프트웨어 |

일반적으로 "서버"라고 하면 "네트워크 연결을 받아들이는 바이너리"와 "머신"을 혼용하지만, Google에서는 이 둘을 엄격히 구분합니다. 하나의 Machine이 어떤 Server든 실행할 수 있으며, 특정 Machine이 특정 Server에 고정되지 않습니다. 리소스 할당은 클러스터 OS인 **Borg**가 담당합니다.

## 하드웨어 구조

Google의 대부분의 컴퓨팅 리소스는 자체 설계 데이터센터에 있으며, 전력 분배·냉각·네트워킹·컴퓨팅 하드웨어 모두 자체 제작합니다.

```
Machine → Rack → Row → Cluster → Datacenter → Campus
```

- **Rack**: 수십 대의 Machine이 배치되는 단위
- **Row**: 하나 이상의 Rack
- **Cluster**: 하나 이상의 Row
- **Datacenter**: 여러 Cluster를 포함하는 건물
- **Campus**: 근접한 여러 Datacenter 건물의 집합

**데이터센터 내 네트워크:**
- **Jupiter**: Google이 만든 수백 개의 스위치를 Clos network fabric으로 연결한 가상 스위치
  - 최대 **1.3 Pbps bisection bandwidth** 지원
  - 수만 개의 포트를 가진 매우 빠른 가상 스위치 역할

**데이터센터 간 네트워크:**
- **B4**: OpenFlow 기반 소프트웨어 정의 네트워크(SDN) 아키텍처
  - 탄력적 대역폭 할당으로 평균 대역폭을 극대화합니다
  - "smart" 라우팅 하드웨어 대신 저렴한 "dumb" 스위칭 컴포넌트 사용

## 시스템 소프트웨어

### Borg — 클러스터 운영체제

Borg는 분산 클러스터 운영체제로, Kubernetes의 직접적인 선조입니다. Apache Mesos와도 유사합니다.

**아키텍처:**

```
┌─────────────────────────────────────┐
│              Cluster                │
│  ┌────────────────────────────────┐ │
│  │           BorgMaster           │ │
│  │  ┌─────────┐  ┌─────────────┐ │ │
│  │  │Scheduler│  │Persistent   │ │ │
│  │  │         │  │Store(Paxos) │ │ │
│  │  └─────────┘  └─────────────┘ │ │
│  └────────────────────────────────┘ │
│         ↕                   ↕       │
│   ┌──────────┐       ┌──────────┐  │
│   │ Borglet  │  ...  │ Borglet  │  │
│   └──────────┘       └──────────┘  │
└─────────────────────────────────────┘
```

- **BorgMaster**: 클러스터 전체 상태를 관리하는 중앙 컨트롤러 (Paxos로 상태 영속화)
- **Scheduler**: Job의 Task를 어떤 Machine에 배치할지 결정
- **Borglet**: 각 Machine에서 실행되며 Task를 실제로 기동·종료·관리

**Job과 Task:**
- **Job**: 하나 이상의 **Task**로 구성된 작업 단위
  - 무한 실행 서버 (예: 웹 서버)
  - 배치 프로세스 (예: MapReduce Job)
- 신뢰성을 위해 단일 Task가 아닌 여러 Task로 구성합니다 (단일 프로세스로 모든 트래픽 처리 불가)
- Borg는 Task를 실행할 Machine을 찾아 서버 프로그램을 기동하고 지속적으로 모니터링합니다
- Task가 오작동하면 kill 후, 필요 시 **다른 Machine**에서 재시작합니다

**BNS (Borg Naming Service):**

Task는 Machine 간 유동적으로 배치되므로 IP 주소와 포트 번호로 참조할 수 없습니다. 이를 해결하기 위해 Job 시작 시 각 Task에 이름과 인덱스 번호를 부여합니다.

- 경로 형식: `/bns/<cluster>/<user>/<job name>/<task number>` → `<IP address>:<port>`로 해석
- BNS 경로는 **Chubby**에 저장됩니다

**리소스 관리:**
- 모든 Job은 필요한 리소스를 명시해야 합니다 (예: CPU 3코어, RAM 2GiB)
- Borg는 이 요구 사항 목록을 바탕으로 Machine에 **binpacking** 방식으로 Task를 최적 배치합니다
- Rack 스위치가 단일 장애점이 되지 않도록 Task를 여러 Rack에 분산 배치합니다
- **요청 자원 초과 시**: Task를 kill하고 재시작합니다 (느리게 크래시루핑하는 Task보다 재시작이 낫습니다)

### Storage 계층

Task는 로컬 디스크를 임시 저장소로 사용할 수 있지만, 영구 저장소로는 클러스터 스토리지 시스템을 사용합니다.

**계층 구조:**

```
Blobstore / Spanner / Bigtable
             ↓
          Colossus       ← GFS의 후계자, 복제·암호화
             ↓
             D           ← 거의 모든 Machine에서 실행되는 fileserver
             ↓
      Local HDD or Flash
```

| 시스템 | 설명 |
|--------|------|
| **D** | 가장 낮은 계층의 fileserver. 디스크와 플래시 스토리지 모두 사용. 클러스터의 거의 모든 Machine에서 실행됨 |
| **Colossus** | D 위에 구축된 클러스터 전체 파일시스템. 일반 파일시스템 시맨틱 + 복제 + 암호화 지원. GFS(Google File System)의 후속 |
| **Bigtable** | Colossus 위에 구축된 페타바이트급 NoSQL 데이터베이스. sparse·distributed·persistent 다차원 정렬 맵. row key, column key, timestamp로 인덱싱. eventually consistent, 크로스 데이터센터 복제 지원 |
| **Spanner** | 전 세계 real-time consistency를 제공하는 SQL-like 인터페이스 DB |
| **Blobstore** | 대용량 파일 저장. 큰 파일은 Colossus에, 작은 파일은 Bigtable에 저장 |

### Networking

**OpenFlow 기반 SDN:**
- "smart" 라우팅 하드웨어 대신 저렴한 "dumb" 스위칭 컴포넌트 사용
- 중앙 컨트롤러(중복 구성)가 네트워크 전체의 최적 경로를 사전 계산
- 비용이 큰 라우팅 결정을 라우터에서 컨트롤러로 이전합니다

**Bandwidth Enforcer (BwE):**
- Borg가 컴퓨팅 자원을 제어하듯, BwE는 네트워크 대역폭을 관리합니다
- 가용 대역폭을 극대화하도록 할당을 최적화합니다
- 분산 라우팅과 트래픽 엔지니어링의 조합으로는 해결하기 매우 어려운 문제들을 중앙화된 트래픽 엔지니어링으로 해결합니다

**GSLB (Global Software Load Balancer):**

전 세계에 분산된 서비스의 레이턴시를 최소화하기 위해 사용자를 가용 용량이 있는 가장 가까운 데이터센터로 라우팅합니다.

3단계 로드밸런싱:

| 단계 | 설명 |
|------|------|
| **DNS 수준** | `www.google.com` 같은 DNS 요청에 대한 지역별 IP 반환 |
| **서비스 수준** | YouTube, Google Maps 등 사용자 서비스 단위 분배 |
| **RPC 수준** | 데이터센터 내부 백엔드 간 RPC 분배 |

서비스 소유자는 서비스의 심볼릭 이름, BNS 주소 목록, 각 위치의 가용 용량(QPS 기준)을 지정하면 GSLB가 BNS 주소로 트래픽을 분배합니다.

### 기타 핵심 소프트웨어

**Chubby — 분산 락 서비스:**
- 파일시스템 같은 API로 락(lock)을 관리하는 서비스
- **Paxos 프로토콜**로 비동기 Consensus를 처리해 데이터센터 간 락을 유지합니다
- **Master election**: 신뢰성을 위해 5개 레플리카로 실행되는 서비스에서 한 번에 한 레플리카만 실제 작업을 수행할 수 있도록 어떤 레플리카가 실행할지 결정합니다
- BNS 경로와 `IP:port` 매핑을 Chubby에 저장합니다 (일관성이 중요한 데이터에 적합)

**Borgmon — 모니터링:**
- 모니터링 대상 서버에서 주기적으로 메트릭을 수집("scrape")합니다
- 수집된 메트릭은 즉각적인 알림에도, 히스토리 그래프 등 장기 보관에도 활용됩니다
- 활용 예:
  - 급성 문제에 대한 알림 설정
  - 소프트웨어 업데이트가 서버 속도를 개선했는지 비교
  - 리소스 소비 패턴 추적 (용량 계획에 필수)

**Stubby — RPC 인프라:**
- Google의 모든 서비스는 **RPC(Remote Procedure Call)** 로 통신합니다
- 오픈소스 버전은 **gRPC**
- 로컬 서브루틴 호출도 RPC로 구현하는 경우가 많습니다 — 나중에 모듈성이 필요할 때 다른 서버로 리팩토링하기 쉽기 때문입니다
- GSLB는 외부 서비스 로드밸런싱과 동일하게 RPC도 로드밸런싱합니다
- 서버는 **frontend**로부터 RPC를 받고, **backend**로 RPC를 보냅니다 (전통적 용어로 frontend=client, backend=server)

**Protocol Buffers (protobuf):**
- RPC 데이터 직렬화 포맷. Apache Thrift와 유사합니다
- XML 대비 장점:
  - 3~10배 더 작은 크기
  - 20~100배 더 빠른 속도
  - 덜 모호한 표현

## 소프트웨어 인프라

- Google의 코드는 **멀티스레드** 방식으로 작성되어 단일 Task가 여러 코어를 효율적으로 활용합니다
- 모든 서버에는 HTTP 서버가 내장되어 있어 진단 정보와 통계를 제공합니다 (대시보드, 모니터링, 디버깅에 활용)

## 개발 환경

- 대부분의 Google Software Engineer는 **단일 공유 코드 저장소** 사용 (Android·Chrome 등 일부 예외)
- 자신의 프로젝트 외부 컴포넌트에서 문제를 발견하면 → 수정 사항을 "changelist(CL)"로 만들어 소유자에게 리뷰 요청 후 메인라인에 반영할 수 있습니다
- **모든 코드 변경(CL)은 코드 리뷰 필수** — 서브밋 전에 반드시 리뷰를 거칩니다
- CL 제출 시 의존 소프트웨어 전체에 대해 자동으로 테스트 수행, 변경이 다른 부분을 깨뜨리면 작성자에게 알림
- **Push-on-green**: 테스트 통과 시 자동으로 프로덕션에 배포하는 방식도 지원

## Shakespeare 예제 — 요청의 생애 주기

셰익스피어의 모든 작품에서 특정 단어가 등장하는 위치를 검색하는 가상의 서비스를 통해 Google 인프라 전체의 동작 방식을 설명합니다.

### 시스템 구성

**두 컴포넌트:**
- **배치 컴포넌트**: 텍스트를 인덱싱해 Bigtable에 저장 (드물게 실행 — 새 텍스트가 발견될 경우에만)
- **프론트엔드 컴포넌트**: 사용자 요청을 실시간으로 처리 (항상 실행 — 모든 시간대의 사용자 대응)

**배치 컴포넌트의 MapReduce 처리 과정:**
1. **Mapping**: 셰익스피어 텍스트를 읽어 개별 단어로 분리 (여러 워커가 병렬 처리)
2. **Shuffle**: 튜플을 단어 기준으로 정렬
3. **Reduce**: `(word, list of locations)` 튜플 생성
4. 각 튜플을 Bigtable에 row로 기록 (단어를 key로 사용)

### 요청 처리 흐름

```
사용자
  ① DNS 쿼리 → Google DNS Server
               (GSLB가 트래픽 부하를 고려해 최적 서버 IP 선택)
  ② HTTP 연결 → GFE (Google Frontend, Reverse Proxy)
               (TCP connection 종료)
  ③ RPC → Application Frontend
               (GSLB로 사용 가능한 Frontend 서버 선택)
  ④ Frontend → GSLB → Application Backend 선택
               (protobuf에 검색할 단어를 담아 전달)
  ⑤ Backend → Bigtable 조회
               (결과를 protobuf로 반환)
  → Frontend가 HTML 조립 → 사용자에게 전달
```

이 모든 과정이 **수백 밀리초** 안에 완료됩니다. 많은 컴포넌트가 관여하므로 잠재적 장애 지점도 많지만, Google의 엄격한 테스트와 신중한 롤아웃 정책, graceful degradation 같은 사전적 오류 복구 방식으로 사용자가 기대하는 신뢰성을 제공합니다.

### 용량 설계 (N+2 원칙)

**단순 계산:**
- 백엔드 1대당 처리 가능: **100 QPS**
- 예상 피크 부하: **3,470 QPS** → 최소 **35대** 필요

**N+2가 필요한 이유:**
- 업데이트 중 1대씩 불가 → 36대로도 부족할 수 있음
- 업데이트 중 Machine 장애 발생 시 35대만 남아 피크 부하를 겨우 처리 → **37대(N+2)** 로 운영

**전 세계 트래픽 분산:**

| 지역 | 예상 QPS | 배치 Task 수 |
|------|---------|-------------|
| 북미 | 1,430 | 17 |
| 유럽·아프리카 | 1,400 | 16 |
| 아시아·오세아니아 | 350 | 6 |
| 남미 | 290 | 4 (N+1로 절충, 용량 부족 시 GSLB가 다른 대륙으로 리다이렉트) |

남미는 N+2 대신 N+1을 선택해 하드웨어 비용을 20% 절감했습니다. 용량이 초과될 경우 GSLB가 다른 대륙으로 트래픽을 리다이렉트하는 대가로 레이턴시를 약간 감수합니다.

**Bigtable 복제:**
- 아시아 백엔드가 미국의 Bigtable을 조회하면 레이턴시가 크게 증가합니다
- 따라서 **각 지역에 Bigtable을 복제**합니다
  - 장점 1: Bigtable 서버 장애 시 복원력 확보
  - 장점 2: 데이터 액세스 레이턴시 감소
- Bigtable은 eventual consistency를 제공하지만, 인덱스 데이터를 자주 업데이트하지 않으므로 문제가 되지 않습니다

> 이 장에서 소개한 용어들은 책 전반에 걸쳐 계속 등장합니다. 모두 기억하지 않아도 되지만, 이후 시스템들을 이해하는 프레임워크로 활용됩니다.
