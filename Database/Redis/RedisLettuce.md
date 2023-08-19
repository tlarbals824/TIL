# Lettuce

Lettuce는 Java로 작성된 비동기 Redis 클라이언트 라이브러리입니다.

> Lettuce 레퍼런스 : https://lettuce.io/core/release/reference/index.html

## Lettuce 특징

* 비동기 방식 : Lettuce는 Netty 기반 비동기 프로그래밍 모델을 지원하여, 여러 Redis 명령을 병렬도 실행하고 응답을 비동기적으로 처리할 수 있습니다. 이는 멀티 스레드 환경에서 더 빠르고 효율적인 Redis 요청 처리를 가능하게 합니다.
* 높은 성능 : Lettuce는 Netty 기반으로하여 높은 성능과 확장성을 제공합니다. 이를 통해 빠른 속도로 Redis와 통신할 수 있습니다.
* 다양한 클라이언트 지원 : Lettuce를 이용하여 클러스터, 센티넬, pipelining, 트랜잭션 그리고 codecs등를 지원합니다.
* 커넥션 핸들링 : standalone 연결 혹은 커넥션 풀의 일부인 커넥션 오브젝트를 통해 Redis로의 커넥션을 관리한다.이 커넥션은 Non-Blocking으로 동작합니다. 또한 커넥션이 더 이상 필요없으면 해제합니다.

## Lettuce 의존성

spring-data-redis 의존성을 추가한다면 자동으로 Lettuce를 사용할 수 있습니다.

~~~
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
// Reactive API
implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
~~~

## Lettuce로 동시성 관리하기

 Lettuce를 이용하면 동시성을 제어할 수 있습니다. 하지만 Redisson의 Pub/Sub 구조와는 다르게 Lettuce는 락 획득을 위한 요청을 지속으로 보내기 때문에 Redis에 부하가 갈 수 있습니다. 이는 [Optimistic Lock](https://github.com/tlarbals824/TIL/tree/main/spring/JPA/JPALock.md)과 비슷하게 동작합니다.

~~~java
public class LettuceConcurrencyControl{
    ...
    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (!redisLockRepository.lock(id)) {
            Thread.sleep(5);
        }
        try {
            stockService.decrease(id, quantity);
        } finally {
            redisLockRepository.unlock(id);
        }
    }
}
~~~
~~~java
public class RedisLockRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public Boolean lock(Long key){
        return redisTemplate.opsForValue()
                .setIfAbsent(generateKey(key), "lock", Duration.ofMillis(3_000));
    }

    public Boolean unlock(Long key){
        return redisTemplate.delete(generateKey(key));
    }

    private String generateKey(Long key){
        return key.toString();
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
> > Lettuce 란? (feat. 레디스의 자바 클라이언트)(https://jake-seo-dev.tistory.com/472)