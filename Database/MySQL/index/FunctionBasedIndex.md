# 함수 기반 인덱스

* 일반적인 인덱스는 칼럼의 값 일부 또는 전체에 대해서만 인덱스 생성이 허용됩니다.
* 때로는 칼럼의 값을 변형해서 만들어진 값에 대해 인덱스를 구축해야 할 때 함수 기반 인덱스를 사용합니다.
* MySQL에서 함수 기반 인덱스를 구현하는 방법은 두 가지로 구분할 수 있습니다.
  * 가상 칼럼을 이용한 인덱스
  * 함수를 이용한 인덱스

## 가상 칼럼을 이용한 인덱스

* MySQL 8.0 버전 이전에는 칼럼의 값을 변형해서 만들어진 값을 인덱스에 등록하기 위해서는 새로운 칼럼을 만들고 모든 레코드에 업데이트한 후에 인덱스를 등록할 수 있었습니다.
* MySQL 8.0 버전부터 가상 칼럼을 추가하고 그 가상 칼럼에 인덱스를 생성할 수 있게 되었습니다.
  * 가상 칼럼은 테이블에 새로운 칼럼을 추가하는 것과 같은 효과를 내기에 실제 테이블의 구조가 변경된다는 단점이 있습니다.

~~~mysql
mysql>  create table user (
        user_id bigint,
        first_name varchar(10),
        last_name varchar(10),
        primary key (user_id)
        );

mysql>  alter table user
        add full_name varchar(30) as (concat(first_name,' ', last_name)) virtual, # 가상 컬럼 추가
        add index ix_fullname(full_name); # 인덱스 설정
~~~

## 함수를 이용한 인덱스

* MySQL 8.0 버전부터 테이블의 구조를 변경하지 않고 함수를 직접 사용하는 인덱스를 생성할 수 있게 되었습니다.
* 함수 기반 인덱스를 제대로 활용하려면 반드시 조건절에 함수 기반 인덱스에 명시된 표현식이 그대로 사용돼야 합니다.

~~~mysql
mysql>  create table user (
        user_id bigint,
        first_name varchar(10),
        last_name varchar(10),
        primary key (user_id),
        index ix_fullname ((concat(first_name,' ',last_name))) # 함수를 직접 사용하는 인덱스
        );
~~~


> 참조
>
> Real MySQL 8.0 1권(https://product.kyobobook.co.kr/detail/S000001766482)