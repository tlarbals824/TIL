# 옵티마이저

> 목차
> 
> [1. ORDER BY 처리(filesort)](https://github.com/tlarbals824/TIL/tree/main/Database/MySQL/Optimizer/OrderByProcessing.md)
> 
> [2. GROUP BY 처리](https://github.com/tlarbals824/TIL/tree/main/Database/MySQL/Optimizer/GroupByProcessing.md)
> 
> [3. DISTINCT 처리](https://github.com/tlarbals824/TIL/tree/main/Database/MySQL/Optimizer/DistinctProcessing.md)
> 
> [4. 임시 테이블](https://github.com/tlarbals824/TIL/tree/main/Database/MySQL/Optimizer/InternalTemporaryTable.md)
> 
> [5. 고급 최적화](https://github.com/tlarbals824/TIL/tree/main/Database/MySQL/Optimizer/AdvancedOptimizer.md)
> 
> [6. 조인 최적화 알고리즘](https://github.com/tlarbals824/TIL/tree/main/Database/MySQL/Optimizer/JoinOptimizeAlgorithm.md)


## 옵티마이저 종류

### 비용 기반 최적화(Cost-based Optimizer, CBO)

* 비용기반 최적화는 쿼리를 처리하기 위한 여러 가지 가능한 방법을 만들고, 각 단위 작업의 비용 정보와 대상 테이블의 예측된 통계 정보를 이용해 실행 계획별 비용을 산출합니다.
* 산출된 실행 방법별로 비용이 최소로 소요되는 처리 방식을 선택해 최종적으로 쿼리를 실행합니다.

### 규칙 기반 최적화(Rule-based Optimizer, RBO)

* 규칙 기반 최적화는 옵티마이저에 내장된 우선순위에 따라 실행 계획을 수립하는 방식을 의미합니다.
* 규칙 기반 최적화 방식은 통계 정보(테이블의 레코드 건수나 칼럼값의 분포도)를 조사하지 않고 실행 계획이 수립되기에 같은 쿼리는 거의 항상 같은 실행 방법이 만들어집니다.
* 옛날 방식이며 현재는 거의 사용하지 않습니다.

## MySQL 기본 데이터 처리

## 풀 테이블 스캔

* 풀 테이블 스캔은 인덱스를 사용하지 않고 테이블의 데이터를 처음부터 끝까지 읽어서 처리하는 작업을 의미합니다.
* MySQL 옵티마이저는 다음과 같은 조건이 일치할 때 주로 풀 테이블 스캔을 합니다.
    * 테이블의 레코드 건수가 너무 작아서 인덱스를 통해 읽는 것보다 풀 테이블 스캔을 하는 편이 더 빠른 경우
    * WHERE 절이나 ON 절에 인덱스를 이용할 수 있는 적절한 조건이 없는 경우
    * 인덱스 레이진 스캔을 사용할 수 있는 쿼리라고 하더라도 옵티마이저가 판단한 조건 일치 레코드 건수가 너무 많은 경우(인덱스의 B-Tree를 샘플링해서 조사한 통계 정보 기준)
* InnoDB 내부적으로 연속된 데이터 페이지가 읽히면 백그라운스 스레드를 이용하여 미리 데이터를 읽어와 버퍼 풀에 가져다 두는 **리드 어헤드(Read ahead)** 작업이 자동으로 발생합니다.

## 풀 인덱스 스캔

* 풀 인덱스 스캔은 인덱스를 처음부터 끝까지 스캔하는 것을 의미합니다.
* 풀 인덱스 스캔도 풀 테이블 스캔과 마찬가지고 InnoDB에서의 **리드 어헤드(Read ahead)** 작업을 사용합니다.

> 참조
>
> Real MySQL 8.0 1권(https://product.kyobobook.co.kr/detail/S000001766482)