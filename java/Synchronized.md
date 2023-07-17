# Synchronized

자바에서 멀티스레드를 이용하면 여러작업을 동시에 진행할 수 있기에 효율이 좋습니다. 하지만 동시에 여러 스레드에서 하나의 작업을 진행한다면 예상하지 못한 일이 발생할 수 있습니다.

## 스레드 동기화란?

스레드 동기화란 java에서 멀티스레드 환경에서 공유된 리소스에 대한 접근을 동기화하는 기술입니다.
java에서는 스레드 동기화를 달성하기 위해 **'synchronized'** 키워드를 사용할 수 있습니다.

Java에서 Synchronized 키워드를 사용하게 되면 멀티 스레드 환경에서 하나의 스레드가 해당 영역을 접근하여 사용하면 Lock이 걸리게 되며 다른 스레드들은 unlock이 될때까지 기다려야합니다. 따라서 해당 영역이 하나의 스레드만 사용함을 보장해줍니다.

## Synchronized 사용방법

### 메소드

메소드 타입 앞에 synchronized 선언을 통해 메서드 전체를 동기화할 수 있습니다.

~~~java
public synchronized void decrease(){
    count--;
    System.out.println(count);
}
~~~

### 코드 블럭

특정 코드만 동기화하고싶다면 synchronized 키워드를 사용하여 해당 코드를 블럭으로 감싸면 됩니다.
이를 통해 특정 코드의 동기화를 달성할 수 있습니다.

~~~java
public void decrease(){
    synchronized(this){
        count--;
    }
    System.out.println(count);
}
~~~

## JPA 동시성 문제에서 Synchronized 사용하기

JPA에서 동시성 문제가 발생할경우 Synchronized를 이용하여 문제를 해결할 수 있습니다. 즉, 여러 스레드에서 하나의 Entity의 재고의 수를 줄이는 경우 하나의 스레드에서만 재고 감소 메서드를 호출하고 반영한다면 동시성 문제를 해결할 수 있습니다.

~~~java
public class SynchronizedConcurrencyControl{
    //    @Transactional
    public synchronized void decrease(Long id, Long quantity) {
        // get stock
        Stock stock = stockRepository.findByProductId(id).orElseThrow(() -> new IllegalArgumentException("재고가 없습니다."));
        // 재고감소
        stock.decrease(quantity);
        //저장
        stockRepository.saveAndFlush(stock);
    }
}
~~~

하지만 동시성 문제를 synchronized를 통해 해결하는 경우 @Transactional을 통해서 트랜잭션을 관리할 수 없습니다. 그 이유는 다음과 같습니다. 

@Transactional이 설정된 메서드는 Proxy 패턴으로 클래스가 생성이 됩니다. 그다음 생성된 클래스의 메서드에서 트랜잭션이 시작하고 기존 메서드가 실행됩니다. 이때 기존 메서드 실행은 Synchornized에 의해서 동기화가 됩니다. 마지막으로 작업이 완료된 이후에 커밋이 됩니다.  

이때 기존 메서드 실행과 트랜잭션 커밋 사이에 시간으로 인해 다른 스레드에서 해당 내용이 반영되지 않은 데이터를 가져오게 되고 동시성 문제가 발생하게 됩니다.
 

~~~java
public class SynchronizedConcurrencyControlWithTransactional{
    private final SynchronizedConcurrencyControl concurrencyControl;
    private final TransactionManager manager = TransactionManager.getInstance();
    
    // 트랜잭션 시작 -> 메서드 실행 -> 다른 스레드에서 메서드 실행 -> 커밋 (동시성 문제 발생!!)
    public void decrease(Long id, Long quantity){
        try{
            manager.begin();
            
            concurrencyControl.decrease(id, quantity);
            
            manager.commit();
        } catch (Exception e){
            manager.rollback();
        }
    }
    
}
~~~

> 동시성 문제 해결하기
>
> * [Java Synchronized](https://github.com/tlarbals824/TIL/tree/main/java/Synchronized.md)
>
> * [JPA Lock](https://github.com/tlarbals824/TIL/tree/main/spring/JPA/JPALock.md)
>
> *


> 참조 : 
> Synchronized(https://kadosholy.tistory.com/123)
> 
> 동시성 제어(https://www.inflearn.com/course/동시성이슈-재고시스템/dashboard)
> 
> @Transactional Proxy(https://minkukjo.github.io/framework/2021/05/23/Spring/)

