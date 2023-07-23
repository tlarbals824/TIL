# Redisson

## Redisson 이란?

Redisson은 Java 기반의 오픈 소스 클라이언트 라이브러리입니다. 또한 다음의 주요 기능을 제공합니다.

* 분산 객체 : Redisson은 자바 객체를 Redis 서버에 저장하고 관리할 수 있도록 도와줍니다. Java 객체의 직렬화와 역직렬화를 자동으로 처리하므로, 개발자는 별도의 직렬화 코드를 작성할 필요 없이 객체를 Redis에 저장하고 조회할 수 있습니다.
* 맵과 집합 : Redisson은 Java의 Map, Set 인터페이스와 유사한 인터페이스를 제공하며, 이를 사용하여 Redis의 맵과 집합을 다룰 수 있습니다. 이를 통해 더 복잡한 자료구조를 쉽게 Redis에 저장하고 조작할 수 있습니다.
* 락 및 동기화 : Redisson은 분산 락을 제공하여 여러 프로세스 또는 서버 간에 안전하게 동기화를 수행할 수 있습니다. 이를 통해 여러 클라이언트가 동일한 자원을 동시에 변경하지 않도록 보호할 수 있습니다.
* 지연 실행 및 반복 작업 : Redisson은 지연 실행 큐와 반복 작업 스케줄링을 지원합니다. 이를 활용하여 특정 작업을 특정 시간에 실행하도록 예약할 수 있으며, 주기적으로 반복 작업을 수행할 수 있습니다.
* 메시징 : Redisson은 Pub/Sub 모델을 지원하여 여러 클라이언트 간에 메시지를 주고받을 수 있도록 합니다. 이를 이용하여 이벤트 기반 시스템을 구축하거나 메시지 큐를 구현할 수 있습니다.

## Redisson으로 분산락 구현

Redisson이 제공하는 락은 내부적으로 Pub/Sub을 이용하여 구현되어 있습니다. 따라서 Lettuce의 스핀락과는 다르게 [Pessimistic Lock](https://github.com/tlarbals824/TIL/tree/main/spring/JPA/JPALock.md)의 방식과 비슷한 락 획득까지 대기하는 방식으로 동작합니다.

이는 충돌이 많은 환경에서 Lettuce 보다 더 좋은 성능을 제공하며 Redis에 대한 더 낮은 부하를 유발합니다.

~~~java
public class RedissonConcurrencyControl{
    private final RedissonClient redissonClient;
    
    public void decrease(Long id, Long quantity) {
        RLock lock = redissonClient.getLock(id.toString());

        try {
            boolean available = lock.tryLock(5, 1, TimeUnit.SECONDS);

            if (!available) {
                System.out.println("lock 획득 실패");
                return;
            }

            stockService.decrease(id, quantity);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
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
> 
> 풀필먼트 입고 서비스팀에서 분산락을 사용하는 방법 - Spring Redisson
(https://helloworld.kurly.com/blog/distributed-redisson-lock/)