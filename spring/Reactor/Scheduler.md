# Scheduler

Reactor의 Scheduler는 비동기 프로그래밍을 위해 사용되는 스레드를 관리해 주는 역할을 합니다. 즉, Scheduler를 사용하여 어떤 스레드에서 무엇을 처리할지 제어합니다.

일반적으로 Java 프로그래밍에서 멀티스레드를 완벽하게 제어하는 것은 쉬운 일이 아닙니다. 스레드의 경쟁 상태(Race Condition)등을 신중하게 고려해서 코드를 작성해야 하는데, 이로 인해 코드의 복잡도가
높아지고 결과적으로 예상치 못한 오류가 발생할 가능성이 높습니다.

Reactor의 Scheduler를 사용하면 이러한 문제를 최소화할 수 있습니다. Scheduler를 사용하면 코드 자체가 매우 간결해지고, Scheduler가 스레드의 제어를 대신해 주기 때문에 개발자가 직접
스레드를 제어할 부담에서 벗어날 수 있습니다.

## subscribeOn()

subscribeOn() Operator는 구독이 발생한 직후 실행될 스레드를 지정하는 Operator입니다.

구독이 발생하면 원본 Publisher가 데이터를 최초로 emit하게 되는데, subscribeOn() Operator는 구독 시점 직후에 실행되기 때문에 원본 Publisher의 동작을 수행하기 위한 스레드를
제어하게 됩니다.

만일 subscribeOn() Operator를 설정하지 않았다면 최초 실행 스레드인 main 스레드에서 Publisher에 대한 처리가 이루어집니다.

~~~java
public class SubscribeOnExample {
    public static void main(String[] args) {
        Flux.fromArray(new Integer[]{1, 3, 5, 7})
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(data -> log.info("# doOnNext: {}", data))
                .doOnSubscribe(subscription -> log.info("# doOnSubscribe"))
                .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(500L);
    }
}
~~~

~~~
19:55 [main] - # doOnSubscribe // 최초 실행 스레드는 main 스레드입니다.
19:55 [boundedElastic-1] - # doOnNext: 1
19:55 [boundedElastic-1] - # onNext: 1
19:55 [boundedElastic-1] - # doOnNext: 3
19:55 [boundedElastic-1] - # onNext: 3
19:55 [boundedElastic-1] - # doOnNext: 5
19:55 [boundedElastic-1] - # onNext: 5
19:55 [boundedElastic-1] - # doOnNext: 7
19:55 [boundedElastic-1] - # onNext: 7
~~~

## publishOn()

Publisher는 Reactor Sequence에서 발생하는 Signal을 Downstream에 전송하는 주체입니다. 이러한 관점에서 publishOn() Operator는 Downstream으로 Signal을
전송할 때 실행되는 스레드를 제어하는 역할을 하는 Operator 입니다.

publishOn() Operator는 코드상에서 publishOn()을 기준으로 아래의 **Downstream의 실행 스레드를 변경**합니다.

~~~java
public class PublishOnExample {
    public static void main(String[] args) {
        Flux.fromArray(new Integer[]{1, 3, 5, 7})
                .doOnNext(data -> log.info("# doOnNext: {}", data))
                .doOnSubscribe(subscription -> log.info("# doOnSubscribe"))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(500L);
    }
}
~~~

~~~
28:28 [main] - # doOnSubscribe
28:28 [main] - # doOnNext: 1
28:28 [main] - # doOnNext: 3
28:28 [main] - # doOnNext: 5
28:28 [main] - # doOnNext: 7
28:28 [parallel-1] - # onNext: 1
28:28 [parallel-1] - # onNext: 3
28:28 [parallel-1] - # onNext: 5
28:28 [parallel-1] - # onNext: 7
~~~

## parallel()

subscribOn(), publishOn() Operator는 동시성을 가지는 논리적인 스레드에 해당하지만 parallel() Operator는 병렬성을 가지는 물리적인 스레드에 해당합니다.

parallel()의 경우 라운드 로빈 방식으로 CPU 코어 개수만큼의 스레드를 병렬로 실행합니다.

parallel() Operator만 추가한다고 해서 emit되는 데이터를 병렬로 처리하지 않습니다. parallel() Operator는 emit되는 데이터를 CPU의 물리적인 스레드의 수에 맞게 골고루 분배하는
역할만 하며, 실제로 병렬 작업을 수행할 스레드의 할당은 runOn() Operator가 담당합니다.

> Reactor에서는 라운드 로빈 방식으로 CPU의 논리적인 코어 수에 맞게 데이터를 그룹화한 것을 'rail' 이라고 합니다.

~~~java
public class ParallelExample {
    public static void main(String[] args) {
        Flux.fromArray(new Integer[]{1, 3, 5, 7, 9, 11, 13, 15, 17, 19})
                .parallel() // emit된 데이터의 분배, 
//                .parallel(4) // 사용하고자 하는 스레드의 개수 지정 가능
                .runOn(Schedulers.parallel()) // 실제 병렬 작업 수행 스레드 할당
                .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(500L);
    }
}
~~~

~~~
34:33 [parallel-6] - # onNext: 11
34:33 [parallel-5] - # onNext: 9
34:33 [parallel-2] - # onNext: 3
34:33 [parallel-3] - # onNext: 5
34:33 [parallel-7] - # onNext: 13
34:33 [parallel-1] - # onNext: 1
34:33 [parallel-2] - # onNext: 19
34:33 [parallel-8] - # onNext: 15
34:33 [parallel-4] - # onNext: 7
34:33 [parallel-1] - # onNext: 17
~~~

## Scheduler 종류

| 종류                    | 설명                                                                                                                                                                                 |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| immediate()           | Schedulers.immediate()는 별도의 스레드를 추가적으로 생성하지 않고, 현재 스레드에서 작업을 처리하고자 할 때 사용합니다.                                                                                                      |
| single()              | Schedulers.single()은 스레드 하나만 생성해서 Scheduler가 제거되기 전까지 재사용하는 방식입니다. 지연 시간이 짧은 작업을 처리하는 것이 효과적입니다.                                                                                   |
| newSingle()           | Schedulers.newSingle()은 호출할 때마다 새로운 스레드 하나를 생성합니다. 첫번째 파라미터는 스레드의 이름을 두번째 파라미터는 데몬 스레드로 동작할지 여부를 설정합니다. (ex: Schedulers.newSingle("new-single", true))                             |
| boundedElastic()      | Schedulers.boundedElastic()은 ExecutorService 기반의 스레드 풀을 생성하여 스레드를 재사용하는 방식입니다. 기본적으로 CPU 코어 * 10만큼 스레드를 생성합니다. 최대 100,000개의 작업이 대기할 수 있습니다. Blocking I/O 작업에서 효과적으로 처리하기 위한 방식입니다. |
| parallel()            | Schedulers.parallel()은 Non-Blocking I/O에 최적화되어 있는 Schedulers로서 CPU 코어 수만큼 스레드를 생성합니다.                                                                                              |
| fromExecutorService() | Schedulers.fromExecutorService()는 기존에 사용하고 있는 ExecutorService가 있다면 이 ExecutorService를 이용하여 Scheduler를 생성하는 방식입니다. 하지만 Reactor에서는 이 방식을 권하지 않습니다.                                   |
| newXXX()              | 기존 single(), boundedElastic(), parallel()에 new를 붙임으로써 새로운 Scheduler 인스턴스를 생성할 수 있습니다. 즉, 사용자 지정 스레드 풀을 생성할 수 있습니다.                                                                 |

> 참고 :
>
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)