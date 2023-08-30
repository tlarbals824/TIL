# GROUP BY 처리

* GROUP BY에 사용된 조건은 인덱스를 사용해서 처리할 수 없습니다. 따라서 WHERE 절을 튜닝하려고 인덱스를 생성할 필요는 없습니다.
* GROUP BY 작업도 인덱스를 사용하는 경우와 그렇지 못한 경우로 나눠 볼 수 있습니다.
  * 인덱스를 사용하는 경우 : 인덱스 스캔 방식, 루스 인덱스 스캔 방식
  * 인덱스를 사용하지 못하는 경우 : 임시 테이블을 사용합니다.

## 인덱스 스캔을 이용하는 GROUP BY(타이트 인덱스 스캔)

* 조인의 드라이빙 테이블에 속한 칼럼만 이용해 그루핑할 때 GROUP BY 칼럼으로 이미 인덱스가 있다면 그 인덱스를 이용하여 그루핑 작업을 수행후 조인을 처리합니다.
* GROUP BY가 인덱스를 사용해 처리된다 하더라도 그룹 함수 등의 그룹값을 처리해야 하는경우 임시 테이블을 사용할 때도 있습니다.
* GROUP BY가 인덱스를 이용해 처리된다면 추가적인 정렬이 필요없습니다.
* 이 방식의 경우 쿼리 실행 계획에서 Extra 칼럼에 별도로 GROUP BY 관련 코멘트가 표시되지 않습니다.

## 루스 인덱스 스캔을 이용하는 GROUP BY

* 루스 인덱스 스캔을 이용하여 GROUP BY를 처리하는 경우 실행 계획의 Extra 칼럼에 "Using idex for group-by" 코멘트가 표시됩니다.
* MySQL의 루스 인덱스 스캔 방식은 단인 테이블에 대해 수행되는 GROUP BY 처리에만 사용할 수 있습니다.
* 프리픽스 인덱스는 루스 인덱스 스캔을 사용할 수 없습니다.
* 루스 인덱스 스캔에서는 유니크한 값의 수가 적을수록 성능이 향상됩니다.
* 루스 인덱스 스캔으로 처리되는 쿼리에서는 별도의 임시 테이블이 필요하지 않습니다.

다음은 루스 인덱스 스캔의 예시이며 실행되는 순서를 나타낸것입니다.
~~~mysql
mysql>  explain 
        select emp_no
        from salaries
        where from_date = '1985-03-01'
        group by emp_no
~~~
~~~
+---+--------+-----+-------+-------------------------------------+
| id|table   |type |key    |Extra                                |
+---+--------+-----+-------+-------------------------------------+
| 1 |salaries|range|primary|Using where; Using index for group-by|
+---+--------+-----+-------+-------------------------------------+
~~~

1. (emp_no, from_date) 인덱스를 차례대로 스캔하면서 emp_no의 첫 번째 유일한 값(그룹키)를 찾습니다.
2. (emp_no, from_date) 인덱스에 찾은 emp_no를 이용하여 from_date가 '1985-03-01'인 레코드를 찾습니다. 이 과정은 결국 "emp_no=찾은값 and from_date='1985-03-01'" 조건으로 인덱스를 이용한 검색과 같습니다.
3. (emp_no, from_date) 인덱스를 차례대로 스캔하면서 emp_no의 다음 키 값을 찾습니다.
4. 2번 과정을 반복합니다.

## 임시 테이블을 사용하는 GROUP BY

* GROUP BY의 기준 칼럼이 드라이빙 테이블에 있든 드리븐 테이블에 있든 관계없이 인덱스를 전혀 사용하지 못할 때는 이 방식으로 처리됩니다.
* 이 방법이 사용되면 실행 계획의 Extra에 "Using Temporary" 메시지가 표시되는데 이는 그루핑 칼럼이 있는 테이블을 풀 스캔(ALL)을 수행해서가 아닌 인덱스를 전혀 사용할 수 없어 임시 테이블을 사용하기 때문입니다.  
* MySQL 8.0 이전 버전에는 묵시적으로 ORDER BY가 사용되지 않더라도 GROUP BY로 그루핑되는 칼럼을 기준으로 묵시적인 정렬을 수행했습니다.
* MySQL 8.0 이후 버전에서는 묵시적 정렬을 수행하지 않습니다.
* MySQL 8.0에서는 GROUP BY가 필요한 경우 내부적으로 GROUP BY 절의 칼럼들로 구성된 유니크 인덱스를 가진 임시 테이블을 만들어서 중복 제거와 집합 함수 연산을 수행합니다.

다음은 임시 테이블을 사용하는 GROUP BY의 예시입니다.
~~~mysql
mysql>  explain 
        select e.last_name, avg(s.salary)
        from employees e, salaries s
        where s.emp_no=e.emp_no
        group by e.last_name
~~~
~~~
+---+--------+-----+-------+---------------+
| id|table   |type |key    |Extra          |
+---+--------+-----+-------+---------------+
| 1 |e       |ALL  |NULL   |Using temporary|
| 1 |s       |ref  |PRIMARY|NULL           |
+---+--------+-----+-------+---------------+
~~~
~~~mysql
# 임시 테이블 생성
create temporary table ... (
    last_name varchar(16),
    salary int,
    unique index ux_lastname (last_name)
)
~~~

> 참조
>
> Real MySQL 8.0 1권(https://product.kyobobook.co.kr/detail/S000001766482)