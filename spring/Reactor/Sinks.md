# Sinks

Java의 [Reactive Streams](https://github.com/tlarbals824/TIL/blob/main/java/ReactiveProgramming/ReactiveStreams/ReactiveStreams.md) 정리에서 Processor는 Publisher와 Subscriber의 기능을 모두 지니고 있다고 설명하고있습니다. Reactor에는 Processor 인터페이스를 구현한 FluxProcessor, MonoProcessor, EmitterProcessor 등을 지원합니다.

하지만 Processor의 기능을 개선한 Sinks가 Reactor 3.4.0 버전부터 지원됨에 따라 Processor와 관련된 API는 Reactor 3.5.0 이후로 완전이 제거될 예정입니다.

## Sinks 란?

>**Sinks are constructs through which Reactive Streams signals can be programmatically pushed, with Flux or Mono semantics.** These standalone sinks expose tryEmit methods that return an Sinks.EmitResult enum, allowing to atomically fail in case the attempted signal is inconsistent with the spec and/or the state of the sink.
 
**"Sinks는 리액티브 스트림즈의 Signal을 프로그래밍 방식으로 푸시할 수 있는 구조이며 Flux, Mono의 의미 체계를 가진다."** 라고 할 수 있습니다.

지금까지는 Flux나 Mono의 onNext 같은 Signal을 내부적으로 전송해 주는 방식이였습니다. 반면, Sinks는 프로그래밍 코드를 통해 명시적으로 Signal을 전송할 수 있습니다.
또한 Reactor에서 Signal을 전송하는 방식인 create(), generate()는 Sinks와 스레드 사용에 있어서의 차이점이 있습니다. create(), generate() Operator는 싱글 스레드 기반으로 전송을하며 Sinks는 멀티 스레드 기반으로 전송하여도 스레드 안전성을 보장합니다.

### create()

create() Operator를 이용하여 간단한 코드를 구성해보았습니다. 이 코드를 실행시킨 결과에서 doTasks() 메소드의 작업이 단일 스레드(boundedElastic-1)에서 처리됨을 알 수 있습니다.
또한 각각의 sequence에서 처리되는 스레드를 다르게 설정하여 총 3개의 스레드(boundedElastic-1, parallel-1, parallel-2)가 동시에 실행됨을 알 수 있습니다.

~~~java
public class SinksExample_Create{
    public static void main(String[] args) {
        int tasks = 6;

        Flux.create((FluxSink<String> sink) -> {
                    IntStream.range(1, tasks)
                            .forEach(n -> sink.next(doTasks(n)));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(n -> log.info("# create(): {}", n))
                .publishOn(Schedulers.parallel())
                .map(result -> result + " success!")
                .doOnNext(n -> log.info("# map(): {}", n))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(500L);
    }

    private static String doTasks(int taskNumber) {
        return "task " + taskNumber + " result";
    }
}
~~~
~~~
32:58 [boundedElastic-1] - # create(): task 1 result
32:58 [boundedElastic-1] - # create(): task 2 result
32:58 [parallel-2] - # map(): task 1 result success!
32:58 [boundedElastic-1] - # create(): task 3 result
32:58 [boundedElastic-1] - # create(): task 4 result
32:58 [parallel-2] - # map(): task 2 result success!
32:58 [boundedElastic-1] - # create(): task 5 result
32:58 [parallel-2] - # map(): task 3 result success!
32:58 [parallel-2] - # map(): task 4 result success!
32:58 [parallel-1] - # onNext: task 1 result success!
32:58 [parallel-2] - # map(): task 5 result success!
32:58 [parallel-1] - # onNext: task 2 result success!
32:58 [parallel-1] - # onNext: task 3 result success!
32:58 [parallel-1] - # onNext: task 4 result success!
32:58 [parallel-1] - # onNext: task 5 result success!
~~~

### Sinks

 앞선 예제에서 create() Operator는 싱글스레드에서 처리함을 알 수 있습니다. 하지만 이 작업을 여러 스레드로 나누어 처리한 후, 결과를 반환하는 상황이 발생할 수 있습니다. 이러한 상황에서 멀티 스레드로 동작하는 Sinks를 사용하면 됩니다.

 아래의 예제 코드에서 확인할 수 있듯이 Sinks를 멀티 스레드 환경에서 구성하여 실행하여도 스레드 안정성을 보장받을 수 있음을 알 수 있습니다.

> 스레드 안전성(Thread Safety) : 스레드 안정성이란 함수나 변수 같은 공유 자원에 동시 접근할 경우에도 프로그램의 실행에 문제가 없음을 의미합니다. 공유 변수에 동시에 접근해서 올바르지 않은 값이 할당된다거나 공유 함수에 동시에 접근함으로써 교착 상태, 즉 Dead Lock에 빠지게 되면 스레드 안전성이 깨지게 됩니다. 

~~~java
public class SinksExample_Sinks{
    public static void main(String[] args) {
        int tasks = 6;

        Sinks.Many<String> unicastSink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<String> fluxView = unicastSink.asFlux(); // Flux(Publisher)로 캐스팅

        IntStream.range(1, tasks)
                .forEach(n -> {
                    try{
                        new Thread(() -> {
                            unicastSink.emitNext(doTasks(n), Sinks.EmitFailureHandler.FAIL_FAST);
                            log.info("# emitted: {}", n);
                        }).start();
                        Thread.sleep(100L);
                    } catch (InterruptedException e){
                        log.error(e.getMessage());
                    }
                });

        fluxView
                .publishOn(Schedulers.parallel())
                .map(result -> result+" success!")
                .doOnNext(n->log.info("# map(): {}",n))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> log.info("# onNext: {}",data));

        Thread.sleep(200L);
    }

    private static String doTasks(int taskNumber) {
        return "task " + taskNumber + " result";
    }
}
~~~
~~~
37:45 [Thread-0] - # emitted: 1
37:45 [Thread-1] - # emitted: 2
37:45 [Thread-2] - # emitted: 3
37:45 [Thread-3] - # emitted: 4
37:45 [Thread-4] - # emitted: 5
37:45 [parallel-2] - # map(): task 1 result success!
37:45 [parallel-2] - # map(): task 2 result success!
37:45 [parallel-1] - # onNext: task 1 result success!
37:45 [parallel-2] - # map(): task 3 result success!
37:45 [parallel-1] - # onNext: task 2 result success!
37:45 [parallel-2] - # map(): task 4 result success!
37:45 [parallel-1] - # onNext: task 3 result success!
37:45 [parallel-1] - # onNext: task 4 result success!
37:45 [parallel-2] - # map(): task 5 result success!
37:45 [parallel-1] - # onNext: task 5 result success!
~~~

## Sinks의 종류

 Reactor에는 Sinks를 사용하여 Signal을 전송할 수 있는 방법이 크게 두 가지입니다. 첫째 Sinks.One을 사용하는 것이고, 둘째는 Sinks.Many를 사용하는 것입니다.

### Sinks One

 Sinks.One은 Sinks.one() 메소드를 사용해서 한 건의 데이터를 전송하는 방법을 정의해 둔 기능 명세라고 말할 수 있습니다.

~~~java
public final class Sinks{
    ...
    public static <T> Sinks.One<T> one() {
        return SinksSpecs.DEFAULT_SINKS.one();
    }
}
~~~

 Sinks.One은 한 건의 데이터를 프로그래밍 방식으로 emit하는 역할을 하기도 하고, Mono 방식으로 Subscriber가 데이터를 소비할 수 있도록 해 주는 Sinks 클래스 내부에 인터페이스로 정의된 Sinks의 스펙 또는 사양으로 불 수 있습니다. 또한 Sinks.One을 Mono로 변환하여 emit된 데이터를 전달받을 수 있으며, 이러한 변환을 "Mono의 의미 체계를 가진다" 라고 표현합니다.

 Sinks.One은 한 건의 데이터만 다루기 때문에 추가적인 데이터의 emit이 있을 경우 Drop이 됩니다.

~~~java
public class SinksOneExample{
    public static void main(String[] args) {
        Sinks.One<String> sinkOne = Sinks.one();
        Mono<String> mono = sinkOne.asMono(); // Sinks.one() -> Mono(Publisher) 변환

        sinkOne.emitValue("Hello Reactor", Sinks.EmitFailureHandler.FAIL_FAST);
//        sinkOne.emitValue("Hi Reactor", Sinks.EmitFailureHandler.FAIL_FAST);

        mono.subscribe(data -> log.info("# Subscriber1: {}",data));
        mono.subscribe(data -> log.info("# Subscriber2: {}",data));
    }
}
~~~
~~~
43:36 [main] - # Subscriber1: Hello Reactor
43:36 [main] - # Subscriber2: Hello Reactor
~~~

### Sinks many()

 Sinks.Many는 Sinks.many() 메소드를 사용하여 여러 건의 데이터를 여러 가지 방식으로 전송하는 기능을 정의해 둔 기능 명세라고 볼 수 있습니다.

~~~java
public final class Sinks{
    ...
    public static ManySpec many() {
        return SinksSpecs.DEFAULT_SINKS.many();
    }

    public interface ManySpec{
        UnicastSpec unicast();
        MulticastSpec multicast();
        MulticastReplaySpec replay();
    }
}
~~~

 Sinks.many() 메소드는 Sinks.one() 메소드와 달리 ManySpec 인터페이스를 반환합니다. Sinks.One의 경우 한 건의 데이터만 다루기에 별도의 Spec이 정의되지 않았지만 Sinks.Many의 경우에는 여러 건의 데이터를 다루기에 데이터 emit을 위한 여러 가지 기능이 필요하여 ManySpec을 정의하여 사용합니다.

Sinks는 Publisher의 역할을 할 경우 기본적으로 Hot Publisher로 동작합니다. 특히 onBackpressureBuffer() 메소드는 Warm up의 특성을 가지는 Hot Sequence로 동작하게 됩니다. Warm up의 경우 [Cold Sequence & Hot Sequence](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/ColdSequenceHotSequence.md)에서 확인할 수 있습니다.


### Sinks many() : unicast()

 UnicastSpec의 기능이 단 하나의 Subscriber에게만 데이터를 emit 합니다. 따라서 Subscriber가 추가된다면 IllegalStateException이 발생하게 됩니다.

~~~java
public class SinksManyExample_Unicast{
    public static void main(String[] args) {
        Sinks.Many<Integer> unicastSink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<Integer> fluxView = unicastSink.asFlux(); // Sinks.many() -> Flux(Publisher) 변환

        unicastSink.emitNext(1, Sinks.EmitFailureHandler.FAIL_FAST);
        unicastSink.emitNext(2, Sinks.EmitFailureHandler.FAIL_FAST);

        fluxView.subscribe(data -> log.info("# Subscriber1: {}",data));

        unicastSink.emitNext(3, Sinks.EmitFailureHandler.FAIL_FAST);

//        fluxView.subscribe(data -> log.info("# Subscriber2: {}",data));
//        Caused by: java.lang.IllegalStateException: Sinks.many().unicast() sinks only allow a single Subscriber
    }
}
~~~
~~~
49:41 [main] - # Subscriber1: 1
49:41 [main] - # Subscriber1: 2
49:41 [main] - # Subscriber1: 3
~~~

### Sinks many() : multicast()

 MulticastSpec의 기능은 하나 이상의 Subscriber에게 데이터를 emit하는 것입니다.

~~~java
public class SinksManyExample_Multicast{
    public static void main(String[] args) {
        Sinks.Many<Integer> multicastSink = Sinks.many().multicast().onBackpressureBuffer();
        Flux<Integer> fluxView = multicastSink.asFlux();

        multicastSink.emitNext(1, Sinks.EmitFailureHandler.FAIL_FAST);
        multicastSink.emitNext(2, Sinks.EmitFailureHandler.FAIL_FAST);

        fluxView.subscribe(data -> log.info("# Subscriber1: {}",data));

        fluxView.subscribe(data -> log.info("# Subscriber2: {}",data));

        multicastSink.emitNext(3, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
~~~
~~~
52:56 [main] - # Subscriber1: 1
52:56 [main] - # Subscriber1: 2
52:56 [main] - # Subscriber1: 3
52:56 [main] - # Subscriber2: 3
~~~


### Sinks many() : replay()

 MulticastReplaySpec은 emit된 데이터를 다시 replay해서 구독 전에 이미 emit된 데이터라도 Subscriber가 전달받을 수 있게 하는 다양한 메서드들이 정의되어 있습니다.
* all() : 구독전 emit된 모든 데이터를 Subscriber에게 전달해줍니다.
* limit() : emit된 데이터 중에서 파라미터로 입력한 개수만큼 가장 나중에 emit된 데이터부터 Subscriber에게 전달하는 기능을 합니다.

~~~java
public class SinksManyExample_Replay{
    public static void main(String[] args) {
        Sinks.Many<Integer> replaySink = Sinks.many().replay().limit(2);
        Flux<Integer> fluxView = replaySink.asFlux();

        replaySink.emitNext(1, Sinks.EmitFailureHandler.FAIL_FAST);
        replaySink.emitNext(2, Sinks.EmitFailureHandler.FAIL_FAST);
        replaySink.emitNext(3, Sinks.EmitFailureHandler.FAIL_FAST);

        fluxView.subscribe(data -> log.info("# Subscriber1: {}",data));

        replaySink.emitNext(4, Sinks.EmitFailureHandler.FAIL_FAST);

        fluxView.subscribe(data -> log.info("# Subscriber2: {}",data));
    }
}
~~~
~~~
55:33 [main] - # Subscriber1: 2
55:33 [main] - # Subscriber1: 3
55:33 [main] - # Subscriber1: 4
55:33 [main] - # Subscriber2: 3
55:33 [main] - # Subscriber2: 4
~~~

### Sinks : FAIL_FAST

위 예제 코드에서 나와있는 FAIL_FAST는 람다 표현식으로 표현한 EmitFailureHandler 인터페이스 구현체입니다.

이 EmitFailureHandler 객체를 통해서 emit 도중 발생한 에러에 대해 빠르게 실패 처리합니다. 빠르게 실패 처리한다는 의미는 **에러가 발생했을 때 재시도를 하지 않고 즉시 실패 처리한다** 를 의미합니다.

~~~java
public final class Sinks{
    ...
    
    public interface EmitFailureHandler{
        EmitFailureHandler FAIL_FAST = (signalType, emission) -> false;
        boolean onEmitFailure(SignalType signalType, EmitResult emitResult);
    }
}
~~~


> 참고 :
>
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)