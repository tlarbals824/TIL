## 인덱스

## 인덱스란?

* 인덱스는 칼럼의 값과 해당 레코드가 저장된 주소를 키와 값의 쌍으로 구성 되어 있습니다.
* 인덱스는 칼럼의 값을 주어진 순서로 미리 정렬해서 보관합니다.
  * 정렬하여 보관하기에 SELECT 쿼리는 빠르게 처리되지만 데이터 저장(INSERT, UPDATE, DELETE) 쿼리의 처리는 느려집니다.
* 인덱스는 데이터를 관리하는 방식(알고리즘)에 따라 대표적으로 B-Tree 인덱스와 Hash 인덱스로 구분할 수 있습니다.
* 데이터의 중복 허용 여부로 분류하면 유니크 인덱스(Unique)와 유니크하지 않은 인덱스(Non-Unique)로 구분할 수 있습니다.

> 목차
> 
> [1. B-Tree]((https://github.com/tlarbals824/TIL/tree/main/Database/MySQL/index/BTreeIndex.md)





