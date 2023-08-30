# DISTINCT 처리

* DISTINCT는 다음과 같은 기준으로 구분할 수 있습니다.
  * MIN(), MAX() 또는 COUNT() 같은 집합 함수와 함께 사용되는 경우
  * 집합 함수가 없는 경우
* 집합 함수와 같이 DISTINCT가 사용되는 쿼리의 실행 계획에서 DISTINCT 처리가 인덱스를 사용하지 못할 때는 항상 임시테이블을 사용합니다. 하지만 실행계획의 Extra 칼럼에는 "Using Temporary" 메시지가 출력되지 않습니다.

## 집합 함수와 함께 사용된 DISTINCT

* COUNT() 또는 MIN(), MAX() 같은 집합 함수 내에서 DISTINCT 키워드가 사용될 수 있습니다.
* 이 경우 집합 함수 내에서 사용된 DISTINCT는 그 집합 함수의 인자로 전달된 칼럼값이 유니크한 것들을 가져옵니다.
* 집합 함수 내에서 DISTINCT 키워드가 사용되는 경우 대상이 되는 테이블의 칼럼을 유니크 인덱스로 가지는 임시 테이블이 생성됩니다. 하지만 실행 계획에서 "Using temporary" 메시지가 출력되지는 않습니다.
~~~mysql
mysql>  explain select count(distinct s.salary)
        from employees e, salaries s 
        where e.emp_no=s.emp_no
        and e.emp_no between 101 and 120;
~~~
~~~
+---+--------+-----+-------+--------------------------------+
| id|table   |type |key    |rows Extra                      |
+---+--------+-----+-------+--------------------------------+
| 1 |e       |range|PRIMARY|100 Using where; Using index    |
| 1 |s       |ref  |PRIMARY|10  NULL                        |
+---+--------+-----+-------+--------------------------------+
~~~

## 집합 함수가 없는 DISTINCT

* SELECT되는 레코드 중에서 유니크한 값을 가져올때 DISTINCT를 사용하면 GROUP BY와 동일한 방식으로 처리 됩니다.
~~~mysql
mysql>  select distinct emp_no from salaries;
mysql>  select emp_no from salaries group by emp_no;
~~~
* DISTINCT는 특정 칼럼에 대해서만 유니크하게 조회하는것이 아닌 SELECT하는 레코드에 대해서 유니크하게 SELECT하는 것입니다. 즉 DISTINCT에 명시된 칼럼에 대해서 유니크한 조합들을 SELECT하는것입니다.

> 참조
>
> Real MySQL 8.0 1권(https://product.kyobobook.co.kr/detail/S000001766482)