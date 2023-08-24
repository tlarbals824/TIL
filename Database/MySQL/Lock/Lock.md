# 락(Lock)

> 목차
> 
> [1. MySQL 엔진 락](https://github.com/tlarbals824/TIL/blob/main/Database/MySQL/Lock/MySQLEngineLock.md)
> 
> [2. InnoDB 스토리지 엔진 락](https://github.com/tlarbals824/TIL/blob/main/Database/MySQL/Lock/InnoDBLock.md)

## MySQL 락

* MySQL에서 사용되는 잠금은 크게 스토리지 엔진 레벨과 MySQL 엔진 레벨로 나눌 수 있습니다.
* MySQL 엔진은 MySQL 서버에서 스토리지 엔진을 제외한 나머지 부분입니다.
* MySQL 엔진 레벨의 잠금은 모든 스토리지 엔진에 영향을 미치지만, 스토리지 엔진 레벨의 잠금은 스토리지 엔진 간 상호 영향을 미치지 않습니다.

> 참조
> 
> Real MySQL 8.0 1권(https://product.kyobobook.co.kr/detail/S000001766482)