# 고급 최적화

* MySQL 서버의 옵티마이저가 실행 계획을 수립할 때 통계 정보와 옵티마이저 옵션을 결합해서 최적의 실핼 계획을 수립하게 됩니다.
* 옵티마이저 옵션은 다음으로 구분할 수 있습니다.
    * 조인 관련 옵티마이저 : 조인 관련 옵티마이저 옵션은 MYSQL 서버 초기 버전부터 제공되던 옵션입니다.
    * 옵티마이저 스위치 : 옵티마이저 스위치는 MySQL 5.5버전부터 지원하였으며 이는 MySQL 서버의 고급 최적화 기능들을 활성화할지를 제어하는 용도로 사용됩니다.

## 옵티마이저 스위치 옵션

* 옵티마이저 스위치 옵션은 optimizer_switch 시스템 변수를 이용해 제어합니다.

| 옵티마이저 스위치 이름                        | 기본값 | 설명                                                            |
|-------------------------------------|:---:|---------------------------------------------------------------|
| batched_key_access                  | off | BKA 조인 알고리즘을 사용하지 여부 설정                                       |
| block_nested_loop                   | on  | Block Nested Loop 조인 알고리즘을 사용할지 여부 설정                         |
| engine_condition_pushdown           | on  | Engine Condition Pushdown 기능을 사용할지 여부 설정                      |
| index_condition_pushdown            | on  | Index Condition Pushdown 기능을 사용할지 여부 설정                       |
| use_index_extensions                | on  | Index Extension 최적화를 사용할지 여부 설정                               |
| index_merge                         | on  | Index Merge 최적화를 사용할지 여부 설정                                   |
| index_merge_intersection            | on  | Index Merge Intersection 최적화를 사용할지 여부 설정                      |
| index_merge_sort_union              | on  | Index Merge Sort Union 최적화를 사용할지 여부 설정                        |
| index_merge_union                   | on  | Index Merge Union 최적화를 사용할지 여부 설정                             |
| mrr                                 | on  | MRR 최적화를 사용할지 여부 설정                                           |
| mrr_cost_based                      | on  | 비용 기반 MRR 최적화를 사용할지 여부 설정                                     |
| semijoin                            | on  | 세미 조인 최적화를 사용할지 여부 설정                                         |
| firstmatch                          | on  | FirstMatch 세미 조인 최적화를 사용할지 여부 설정                              |
| loosescan                           | on  | LooseScan 세미 조인 최적화를 사용할지 여부 설정                               |
| materialization                     | on  | Materialization 최적화를 사용할지 여부 설정(Materialization 세미 조인 최적화 포함) |
| subquery_materialization_cost_based | on  | 비용 기반의 Materialization 최적화를 사용할지 여부 설정                        |

## MRR과 배치 키 액세스(mrr & batched_key_access)

* MySQL 서버에서 지금까지 지원하던 조인 방식은 드라이빙 테이블의 레코드를 한 건 읽어서 드리븐 테이블의 일치하는 레코드를 찾아서 조인을 수행하는 것입니다. (네스티드 루프 조인, Nested Loop Join)
* MySQL 서버의 내부 구조상 조인 처리는 MySQL 엔진이 처리하며, 실제 레코드를 검색하고 읽는 부분은 스토리지 엔진이 담당합니다.
* 드라이빙 테이블의 레코드 건별로 드리븐 테이블의 레코드를 찾으면 레코드를 찾고 읽는 스토리지 엔진에서는 아무런 최적화를 수행할 수 없습니다. 즉, 드리븐 테이블에 대한 풀 테이블 스캔이 반복적으로 발생할 수 있습니다.
* 앞선 단점을 보완하고자 MySQL 서버는 조인 대상 테이블 중 하나로부터 레코드를 읽어서 조인 버퍼에 버퍼링합니다. 이를 통해 버퍼에 있는 레코드를 스토리지 엔진에 한번에 요청하여 테이블을 스캔하는 작업의 수를 줄일 수 있습니다.
* 이러한 방식을 MRR이라 하며, MRR은 "Multi-Range Read"를 줄여서 부르는 이름입니다.
* MRR을 응용해서 실행되는 조인 방식을 BKA(Batched Key Access) 조인이라고 합니다.

## 블록 네스티드 루프 조인(block_nested_loop)

* MySQL 서버에서 사용되는 대부분의 조인은 네스티드 루프 조인(Nested Loop Join)인데, 조인의 연결 조건이 되는 칼럼에 모두 인덱스가 있는 경우 사용되는 조인 방식입니다.
* 네스티드 루프 조인과 블록 네스티드 루프 조인(Block Nested Loop Join)의 가장 큰 차이점은 조인 버퍼가 사용되는지 여부와 조인에서 드라이빙 테이블과 드리븐 테이블이 어떤 순서로 조인되느냐 입니다.
* 조인은 드라이빙 테이블에서 일치하는 레코드의 건수만큼 드리븐 테이블을 검색하면서 처리됩니다. 즉, 드라이빙 테이블은 한번 읽지만, 드리븐 테이블은 여러 번 읽는 것을 의미합니다.
* 어떤 방식으로도 드리븐 테이블의 풀 테이블 스캔이나 인덱스 풀 스캔을 피할 수 없다면 옵티마이저는 드라이빙 테이블에서 읽은 레코드를 메모리에 캐시한 후 드리븐 테이블과 이 메모리 캐시를 조인하는 형태로 처리합니다. 이때 사용되는 메모리의 캐시를 조인 버퍼(Join Buffer)라고 합니다.
* 블록 네스티드 루프 조인에서의 드리븐 테이블과 메모리 캐시를 조인하는 순서는 기존 조인의 순서인 드라이빙 테이블에서 드리븐 테이블의 순서와는 반대로 드리븐 테이블을 읽고 조인 버퍼에 일치하는 레코드를 찾는 방식으로 수행됩니다.

## 인덱스 컨디션 푸시다운(index_condition_pushdown)

* MySQL 5.6 버전부터는 인덱스 컨디션 푸시다운(Index Condition Pushdown)이라는 기능이 도입됐습니다.
* MySQL에서 인덱스를 비교하는 작업은 실제 InnoDB 스토리지 엔진에서 수행하지만 테이블의 레코드에서 'Like'와 같은 비교 작업은 MySQL 엔진이 수행합니다.
* MySQL 5.6 이전 버전에서는 인덱스를 범위 제한 조건(BETWEEN, LIKE, IN)으로 사용하지 못하는 조건은 MySQL 엔진이 스토리지 엔진으로 아예 전달해주지 않았습니다. 
* MySQL 5.6 버전부터는 인덱스를 범위 제한 조건으로 사용하지 못한다고 하더라도 인덱스에 포함된 칼럼의 조건이 있다면 모두 같이 모아서 스토리지 엔진으로 전달할 수 있게 핸들러 API가 개선 됐습니다.(인덱스 컨디션 푸시다운)
* 인덱스 컨디션 푸시다운이 사용되면 실행 계획의 Extra 칼럼에 "Using index condition" 메시지가 출려됩니다.

## 인덱스 확장(use_index_extensions)

* use_index_extensions 옵티마이저 옵션은 InnoDB 스토리지 엔진을 사용하는 테이블에서 세컨더리 인덱스에 자동으로 추가된 프라이머리 키를 활용할 수 있게 할지 결정하는 옵션입니다.
* InnoDB에서의 예로 클러스터링 인덱스가 있습니다. 
* InnoDB의 경우 PK를 클러스터링 인덱스로 등록하며 세컨더리 인덱스 리프 노드에 PK를 가집니다. 따라서 use_index_extensions 옵션을 통해서 InnoDB가 자동으로 추가한 PK를 MySQL 옵티마이저가 인지하고 실행 계획을 수립하도록 할 수 있습니다. 

## 인덱스 머지(index_merge)

* 인덱스를 이용해 쿼리를 실행하는 경우, 대부분 옵티마이저는 테이블별로 하나의 인덱스만 사용하도록 실행 계획을 수립합니다.
* 인덱스 머지 실행 계획을 사용하면 하나의 테이블에 대해 2개 이상의 인덱스를 이용해 쿼리를 처리합니다.
* 쿼리에 사용된 각각의 조건이 서로 다른 인덱스를 사용할 수 있고 그 조건을 만족하는 레코드 건수가 많을 것으로 예상될 때 MySQL서버는 인덱스 머지 실행 계획을 선택합니다.
* 인덱스 머지 실행 계획은 다음과 같이 3개의 세부 실행 계획으로 나눌 수 있습니다.
  * index_merge_intersection
  * index_merge_sort_union
  * index_merge_union

### 인덱스 머지 - 교집합(index_merge_intersection)

* 인덱스 머지 교집합(index_merge_intersection)은 실행 계획 Extra 칼럼에 "Using intersect" 메시지가 출력되며 이 쿼리가 여러 개의 인덱스를 각각 검색해서 그 결과를 교집합만 반환했음을 의미합니다.

### 인덱스 머지 - 합집합(index_merge_union)

* 인덱스 머지 합집합(index_merge_union)은 WHERE 절에 사용된 2개 이상의 조건이 각각의 인덱스를 사용하되 OR 연산자로 연결된 경우에 사용되는 최적화입니다.
* 실행 계획에서 Extra 칼럼에 "Using union(a, b)" 메시지가 출력되면 인덱스 머지 최적화가 a 인덱스의 결과와 b 인덱스 검색 결과를 'Union' 알고리즘으로 병합했음을 의미합니다. 이때 병합은 두 집합의 합집합을 가져왔다는 것을 의미합니다.
* 병합하는 과정에서 정렬된 두 집합의 결과의 중복을 제거할때 우선순위 큐 알고리즘을 사용하여 수행합니다.

### 인덱스 머지 - 정렬 후 합집합(index_merge_sort_union)

* 인덱스 머지 작업을 하는 도중에 결과의 정렬이 필요한 경우 MYSQL 서버는 인덱스 머지 최적화 'Sort union' 알고리즘을 사용합니다.
* 인덱스 머지 정렬 후 합집합이 수행되면 실행 계획의 Extra 칼럼에 "Using sort union" 메시지가 출력됩니다.

## 세미 조인(semijoin)

* 다른 테이블과 실제 조인을 수행하지 않고, 단지 다른 테이블에서 조건에 일치하는 레코드가 있는지 없는지만 체크하는 형태의 쿼리를 세미 조인(Semi-Join)이라고 합니다.
* 세미 조인(Semi-Join) 형태의 쿼리와 안티 세미 조인(Anti Semi-Join) 형태의 쿼리는 최적화 방법이 조금 차이가 있습니다.
* "= (subquery)" 형태와 "IN (subquery)" 형태의 세미 조인 쿼리에 대해 다음과 같이 3가지 최적화 방법을 적용할 수 있습니다.
  * 세미 조인 최적화
  * IN-to-EXSITS 최적화
  * MATERIALIZATION 최적화
* "<> (subquery)" 형태와 "NOT IN (subquery)" 형태의 안티 세미 조인 쿼리에 대해서는 다음 2가지의 최적화 방법이 있습니다.
  * IN-to-EXSITS 최적화
  * MATERIALIZATION 최적화
* MySQL 8.0 버전부터 세미 조인 쿼리의 성능을 개선하기 위한 다음과 같은 최적화 전략이 있습니다.
  * Table Pull-out
  * Duplicate Weed-out
  * First Match
  * Loose Scan
  * Materialization

### 테이블 풀-아웃(Table Pull-out)

* Table pullout 최적화는 세미 조인의 서브쿼리에 사용된 테이블을 아우터 쿼리로 끄집어낸 후에 쿼리를 조인 쿼리로 재작성하는 형태의 최적화입니다.
* 이는 서브쿼리 최적화가 도입되기 이전에 수동으로 쿼리를 튜닝하던 대표적인 방법이었습니다.
* Table pullout 최적화는 별도로 실행 계획의 Extra 칼럼에 "Using table pullout"과 같은 문구가 출려되지 않습니다.
* Table pullout 최적화의 몇 가지 제한 사항과 특성은 다음과 같습니다.
  * Table pullout 최적화는 세미 조인 서브쿼리에서만 사용 가능합니다.
  * Table pullout 최적화는 서브쿼리 부분이 UNIQUE 인덱스나 프라이머리 키 룩업으로 결과가 1건인 경우에만 사용가능합니다.
  * Table pullout이 적용된다고 하더라도 기존 쿼리에서 가능했던 최적화 방법이 사용 불가능한 것은 아니므로 MySQL에서는 가능하다면 Table pullout 최적화를 최대한 적용합니다.
  * Table pullout 최적화는 서브쿼리의 테이블을 아우터 쿼리로 가져와서 조인으로 풀어쓰는 최적화를 수행하는데, 만약 서브쿼리의 모든 테이블이 아우터 쿼리로 끄집어 낼 수 있다면 서브쿼리 자체는 없어집니다.
  * MySQL에서는 "최대한 서브쿼리를 조인으로 풀어서 사용해라"라는 튜닝 가이드가 많은데, Table pullout 최적화는 사실 이 가이드를 그대로 실행하는 것입니다. 이제부터는 서브쿼리를 조인으로 풀어서 사용할 필요가 없습니다.

### 퍼스트 매치(firstmatch)

* firstmatch 최적화 전략은 IN (subquery) 형태의 세미 조인을 EXISTS (subquery) 형태로 튜닝한 것과 비슷한 방법으로 실행됩니다.
* First Match 최적화는 MySQL 5.5에서 수행했던 최적화 방법인 IN-to-EXISTS 변환과 거의 비슷한 처리 로직을 수행합니다.
* MySQL 5.5의 IN-to-EXISTS 변환에 비해 First Match 최적화 전략은 다음과 같은 장점이 있습니다.
  * 기존 IN-to-EXISTS 최적화에서 등등 조건 전파(Equality propagation)가 서브쿼리 내에서만 가능했지만 First Match 최적화는 아우터 쿼리의 테이블까지 전파가능합니다. 최종적으로 First Match 최적화로 실행되면 더 많은 조건이 주어지는 것이므로 더 나은 실행 계획을 수립할 수 있습니다.
  * IN-to-EXISTS 변환 최적화 전략에서는 아무런 조건 없이 변환이 가능한 경우 무조건 그 최적화를 수행했습니다. 하지만 FirstMatch 최적화에서는 서브 쿼리의 모든 테이블에 대해 FirstMatch 최적화를 수행할지 아니면 일부 테이블에 대해서만 수행할지 취사선택할 수 있다는 것이 장점입니다.
* FirstMatch 최적화 또한 특정 형태의 서브쿼리에서 자주 사용되는 최적화입니다. FirstMatch 최적화의 몇가지 제한 사항과 특성은 다음과 같습니다.
  * FirstMatch는 서브쿼리에서 하나의 레코드만 검색되면 더이상의 검색을 멈추는 단축 실행 경로(Short-cut path)이기 때문에 FirstMatch 최적화에서 서브쿼리는 그 서브쿼리가 참조하는 모든 아우터 테이블이 먼저 조회된 이후 실행됩니다.
  * FirstMatch 최적화가 사용되면 실행 계획의 Extra 칼럼에는 "FirstMatch(table-N)" 문구가 표시됩니다.
  * FirstMatch 최적화는 상관 서브쿼리(Correlated subquery)에서도 사용될 수 있습니다.
  * FirstMatch 최적화는 GROUP BY나 집합 함수가 사용된 서브쿼리의 최적화에는 사용될 수 없습니다.

### 루스 스캔(loosescan)

* 세미 조인 서브쿼리 최적화의 LooseScan은 인덱스를 사용하는 GROUP BY 최적화 방법에서 살펴본 "Using Index for group-by"의 루스 인덱스 스캔과 비슷한 방식을 사용합니다.
* 루스 스캔(LooseScan) 최적화는 다음과 같은 특성을 가집니다.
  * LooseScan 최적화는 루스 인덱스 스캔으로 서브 쿼리 테이블을 읽고, 그 다음으로 아우터 테이블을 드리븐으로 사용해서 조인을 수행합니다. 그래서 서브쿼리 부분이 루스 인덱스 스캔을 사용할 수 있는 조건이 갖춰져야 사용할 수 있는 최적화입니다. 루스 인덱스 스캔 최적화는 다음과 같은 형태의 서브쿼리들에서 사용할 수 있습니다.
  * ~~~mysql
    select ... from ... where expr in (select keypart1 from tab where ...)
    select ... from ... where expr in (select keypart1 from tab where keypart1='상수'...)
    ~~~
    
### 구체화(Materialization)

