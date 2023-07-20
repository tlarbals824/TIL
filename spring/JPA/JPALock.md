# JPA Lock

JPA를 이용하여 동시성 문제를 해결하는 방법은 Pessimistic Lock, Optimistic Lock과 Named Lock을 이용하는 방법이 있습니다.

## Pessimistic Lock(비관적 락)

### Pessimistic Lock 이란?

* 선점 잠금이라고 불리기도 합니다.
* 트랜잭션끼리의 충돌이 발생한다고 가정하고 우선 락을 거는 방법입니다.
* 데이터베이스에서 제공하는 락 기능을 사용합니다.

### Pessimistic Lock 장점

* JPA에서 제공하는 락 기능을 사용하기 때문에 구현이 간단합니다.
* 데이터 일관성을 보장해줍니다.
* 충돌이 발생하면 트랜잭션을 롤백시킴으로써 충돌을 처리할 수 있습니다. 이는 데이터 일관성을 유지하는 데 도움을 줍니다.
* 상대적으로 충돌이 많이 발생한다면 낙관적 락(Optimistic Lock) 보다 성능이 좋을 수 있습니다.

### Pessimistic Lock 단점

* 비관적 락은 데이터를 잠그고 대기하는 방식으로 동작합니다. 따라서 다른 트랜잭션에서 데이터를 수정 중인 경우, 락을 획득하기 위해 대기해야 합니다. 이로 인해 성능 저하가 발생할 수 있습니다.

### Pessimistic Lock 종류

* PESSIMISTIC_WRITE(배타적 잠금) : 일반적인 비관적 락 옵션입니다. 데이터베이스에 쓰기 락을 걸때 사용합니다. 이를통해 NON-RERATABLE READ를 방지합니다. 락이 걸린 로우는 다른 트랜잭션이 읽기, 쓰기를할 수 없습니다.
* PESSIMISTIC_READ(공유 잠금) : 데이터를 반복 읽기만 하고 수정하지 않는 용도로 락을 걸 때 사용합니다. 락이 걸린 로우는 다른 트랜잭션이 읽기는 할 수 있습니다.
* PESSIMISTIC_FORCE_INCREMENT : 비관적 락중 유일하게 버전 정보를 사용합니다. 


### Pessimistic Lock 사용법(@Lock)

~~~java
public interface StockRepository extends JpaRepository<Stock, Long> {
    /**
     * 비관적 락 설정
     */
    @Query("select s from Stock s where s.productId = :productId")
    @Lock(value = LockModeType.PESSIMISTIC_WRITE) // 비관적 락 설정
    Optional<Stock> findByProductIdWithPessimisticLock(Long productId);

}
~~~

## Optimistic Lock(낙관적 락)

### Optimistic Lock 이란?

* 트랜잭션 대부분은 충돌이 발생하지 않는다고 낙관적으로 가정하는 방법입니다.
* 데이터베이스가 제공하는 락 기능을 사용하는 것이 아니라 JPA가 제공하는 버전 관리 기능을 사용합니다.
* 낙관적 락은 트랜잭션을 커밋하기 전까지는 트랜잭션의 충돌을 알 수 없습니다.
* 최초 커밋만 인정하기가 적용된다.

### Optimistic Lock 장점

* 별도의 Lock을 걸지 않기 때문에 성능상의 이점이 있습니다.
* Optimistic Lock은 여러 트랜잭션이 동시에 데이터를 수정할 수 있는 환경에서 충돌을 탐지하고 처리할 수 있습니다. 이를통해 동시성을 허용하면서 데이터의 일관성을 유지할 수 있습니다.

### Optimistic Lock 단점

* 충돌에 대한 추가적인 로직을 작성해야 합니다.
* 충돌이 빈번하게 일어난다면 비관적 락이 더 좋은 선택일 수 있습니다.

### Optimistic Lock 종류

Optimistic Lock의 경우 @Version 만 설정해줘도 적용된다.

* NONE : 락 옵션을 적용하지 않아도 엔티티에 @Version이 있다면 Optimistic Lock이 적용됩니다. 엔티티의 값을 변경하면 @Version 설정 값이 증가합니다.
* OPTIMISTIC : @Version만 적용한다면 엔티티를 수정해야 버전을 체크하지만 이 옵션을 추가하면 엔티티를 조회만 해도 버전을 체크합니다. 이는 DIRTY READ, NON-REPEATABLE READ를 방지합니다.
* OPTIMISTIC_FORCE_INCREMENT : OPTIMISTIC LOCK을 사용하면서 버전 정보를 강제로 증가합니다. OPTIMISTIC_FORCE_INCREMENT 옵션은 연관관계에서 한쪽 엔티티에서 변경이 된다면 연관관계로 묶여있는 엔티티들 모두 버전 정보를 증가시킵니다. 이를통해 논리적인 단위의 엔티티 묶음의 버전 관리를 할 수 있습니다. 


### Optimistic Lock 사용법(@Version)

**@Version은 JPA에서 관리하는 필드이기 때문에 개발자가 임의로 수정해서는 안된다.**

~~~java
@Entity
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;
    private Long productId;
    private Long quantity;

    /**
     * 낙관적 락 설정
     */
    @Version
    private Long version;
}
~~~
~~~java
public interface StockRepository extends JpaRepository<Stock, Long> {
    /**
     * 낙관적 락 설정
     */
    @Lock(value = LockModeType.OPTIMISTIC)
    @Query("select s from Stock s where s.productId = :productId")
    Optional<Stock> findByProductIdWithOptimisticLock(Long productId);

}
~~~
~~~java
public class OptimisticLockExample{
    ...
    
    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (true) {
            try {
                optimisticLockStockService.decrease(id, quantity);

                break;
            } catch (Exception e) {
                Thread.sleep(50);
            }
        }
    }
}
~~~


## Named Lock(MySQL USER-LEVEL LOCK)


### Named Lock 이란?

* Named Lock은 고유한 이름 또는 식별자를 가진 락을 사용하여 데이터의 일관성과 동시성을 보장하는 방법입니다.
* Named Lock은 분산락으로 활용할 수 있습니다. 분산락은 여러 서버, 인스턴스 간에 일관성과 동시성을 보장하기 위해 사용되는 락의 형태입니다.

### Named Lock 장점

* Named Lock은 여러 서버 또는 인스턴스 간에 동시성 제어를 위한 락을 제공하여 분산 시스템 환경에서 데이터 일관성을 보장할 수 있습니다.
* JPA의 NativeQuery를 통해 간단하게 구현할 수 있습니다.
* 추가적인 기술 도입없이 사용할 수 있습니다.

### Named Lock 단점

* 단일 데이터베이스 종속성이 문제가 됩니다. 만약 MySQL이 아닌 다른 데이터베이스로 변경한다면 기존 로직이 동작하지 않을 수 있습니다.
* Named Lock 관련 데이터 소스를 분리하여 관리해야합니다. 만일 하나의 데이터 소스를 통해 커넥션을 관리한다면 Named Lock에 의한 커넥션에 의해 모든 커넥션이 고갈될 수 있습니다.

### 

~~~java
public interface LockRepository extends JpaRepository<Stock, Long> {

    @Query(value = "select get_lock(:key, 3000)", nativeQuery = true)
    void getLock(@Param("key") String key);

    @Query(value = "select release_lock(:key)", nativeQuery = true)
    void releaseLock(@Param("key") String key);
}
~~~
~~~java
public class NamedLockExample{
    
    ...
    
    @Transactional
    public void decrease(Long id, Long quantity){
        try{
            lockRepository.getLock(id.toString());
            stockService.decrease(id, quantity);
        }finally {
            lockRepository.releaseLock(id.toString());
        }
    }
}
~~~



> 동시성 문제 해결하기
> * [Java Synchronized](https://github.com/tlarbals824/TIL/tree/main/java/Synchronized.md)
> * [JPA Lock](https://github.com/tlarbals824/TIL/tree/main/spring/JPA/JPALock.md)
> * [Redis Lettuce](https://github.com/tlarbals824/TIL/tree/main/Redis/RedisLettuce.md)
> * [Redis Redisson](https://github.com/tlarbals824/TIL/tree/main/Redis/RedisRedisson.md)

> 참고 :
> 동시성 제어(https://www.inflearn.com/course/동시성이슈-재고시스템/dashboard)
> 
> Pessimistic Lock(https://isntyet.github.io/jpa/JPA-비관적-잠금(Pessimistic-Lock)/)
> 
> 분산락 구현:MySQL(https://techblog.woowahan.com/2631/)
> 
> 분산락 구현:Redisson(https://helloworld.kurly.com/blog/distributed-redisson-lock/)