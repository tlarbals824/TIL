# Debugging

Spring MVC와 같이 흔히 사용하는 동기식 프로그래밍에서는 예외가 발생하면 스택트레이스를 확인하거나 예외 발생이 예상되는 코드에 브레이크포인트를 걸어서 디버깅을 진행합니다. 반면 Reactor와 같은 비동기 프로그래밍 환경에서 선언적으로 코드를 작성하기에 디버깅이 쉽지 않습니다. 이러한 문제를 해결하기 위해 Reactor는 다양한 디버깅 도구를 제공합니다. 

## Hooks.onOperatorDebug() 사용하기

Reactor에서는 디버그 모드를 활성화하여 Reactor Sequence를 디버깅할 수 있습니다. 이를 위해 Hooks.onOperatorDebug() 메소드를 선언하면 디버그 모드를 활성화할 수 있습니다. 이렇게 디버그 모드를 활성화한다면 기존의 위치를 알 수 없는 에러 메시지에 추가적인 정보가 추가됩니다. 이 추가된 정보에는 예외가 발생한 지점을 포함합니다.

Hooks.onOperatorDebug()은 다음과 같은 비용이 많이드는 과정을 거칩니다.
1. 애플리케이션 내에 있는 모든 Operator의 스택트레이스를 캡쳐합니다.
2. 에러가 발생하면 캡처한 정보를 기반으로 에러가 발생한 ASSEMBLY의 스택트레이스를 원본 스택트레이스 중간에 끼워 넣습니다.

따라서 에러 원인을 추적하기 위해서 처음부터 디버그 모드를 활성화하는 것은 권장하지 않습니다.

~~~java
public class DebuggingExample {

    public static Map<String, String> fruits = new HashMap<>();

    static {
        fruits.put("banana","바나나");
        fruits.put("apple","사과");
        fruits.put("pear","배");
        fruits.put("grape","포도");
    }

    public static void main(String[] args) {
//        Hooks.onOperatorDebug(); // 디버깅 모드 활성화

        Flux.fromArray(new String[]{"banana","apple","pear","melon"})
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())
            .map(fruits::get)
            .map(fruit -> fruit + "가 나왔습니다.")
            .subscribe(log::info,
                error -> log.error("# onError: {}",error));

        Thread.sleep(100L);
    }
}
~~~
#### Hooks.onOperatorDebug() 미사용 로그
~~~
37:58 [parallel-1] - 바나나가 나왔습니다.
37:58 [parallel-1] - 사과가 나왔습니다.
37:58 [parallel-1] - 배가 나왔습니다.
37:58 [parallel-1] - # onError: 
java.lang.NullPointerException: The mapper [com.sim.webflux.WebfluxApplication$$Lambda$201/0x000000d80114d3b0] returned a null value
~~~
#### Hooks.onOperatorDebug() 사용 로그
~~~
38:28 [parallel-1] - 바나나가 나왔습니다.
38:28 [parallel-1] - 사과가 나왔습니다.
38:28 [parallel-1] - 배가 나왔습니다.
38:28 [parallel-1] - # onError: 
java.lang.NullPointerException: The mapper [com.sim.webflux.WebfluxApplication$$Lambda$205/0x000000700114e438] returned a null value.
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:115)
	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Assembly trace from producer [reactor.core.publisher.FluxMapFuseable] :
	reactor.core.publisher.Flux.map(Flux.java:6514)
	com.sim.webflux.WebfluxApplication.main(WebfluxApplication.java:36)
Error has been observed at the following site(s):
	*__Flux.map ⇢ at com.sim.webflux.WebfluxApplication.main(WebfluxApplication.java:36) // 예외발생지점
	|_ Flux.map ⇢ at com.sim.webflux.WebfluxApplication.main(WebfluxApplication.java:37)
Original Stack Trace: ...
~~~


## checkpoint() 사용하기

디버그 모드를 활성화하는 방법이 애플리케이션 내에 있는 모든 Operator에서 스택트레이스를 캡쳐하는 반면에, checkpoint() Operator를 사용하면 특정 Operator 체인 내의 스택트레이스만 캡처합니다.

### checkpoint() 사용하기 : 파라미터 입력 x

checkpoint()를 사용하면 실제 에러가 발생한 assembly 지점 또는 에러가 전파된 assembly 지점의 Traceback이 추가됩니다.

~~~java
public class CheckPointExample{
    public static void main(String[] args) {
        Flux.just(2, 4, 6, 8)
            .zipWith(Flux.just(1, 2, 3, 0), (x, y) -> x / y)
            .checkpoint() // 최초 예외 발생 감지
            .map(num -> num + 2)
            .checkpoint()
            .subscribe(
                data -> log.info("# onNext: {}", data),
                error -> log.info("# onError: ", error)
            );

        Thread.sleep(100L);
    }
}
~~~
~~~
44:27 [main] - # onNext: 4
44:27 [main] - # onNext: 4
44:27 [main] - # onNext: 4
44:27 [main] - # onError: 
java.lang.ArithmeticException: / by zero
	at com.sim.webflux.WebfluxApplication.lambda$main$0(WebfluxApplication.java:29)
	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Assembly trace from producer [reactor.core.publisher.FluxZip] :
	reactor.core.publisher.Flux.checkpoint(Flux.java:3556)
	com.sim.webflux.WebfluxApplication.main(WebfluxApplication.java:30)
Error has been observed at the following site(s):
	*__checkpoint() ⇢ at com.sim.webflux.WebfluxApplication.main(WebfluxApplication.java:30)
	|_ checkpoint() ⇢ at com.sim.webflux.WebfluxApplication.main(WebfluxApplication.java:32)
Original Stack Trace: ...
~~~

### checkpoint() 사용하기 : Traceback 출력 없이 Description 출력

checkpoint(description)를 사용하면 에러 발생 시 Traceback을 생략하고 description을 통해 에러 발생 지점을 예상할 수 있습니다.

~~~java
public class CheckPointExample{
    public static void main(String[] args) {
        Flux.just(2, 4, 6, 8)
            .zipWith(Flux.just(1, 2, 3, 0), (x, y) -> x / y)
            .checkpoint("CheckpointExample.zipWith.checkpoint")
            .map(num -> num + 2)
            .checkpoint("CheckpointExample.map.checkpoint")
            .subscribe(
                data -> log.info("# onNext: {}", data),
                error -> log.info("# onError: ", error)
            );

        Thread.sleep(100L);
    }
}
~~~
~~~
46:56 [main] - # onNext: 4
46:56 [main] - # onNext: 4
46:56 [main] - # onNext: 4
46:56 [main] - # onError: 
java.lang.ArithmeticException: / by zero
	at com.sim.webflux.WebfluxApplication.lambda$main$0(WebfluxApplication.java:29)
	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Error has been observed at the following site(s):
	*__checkpoint ⇢ CheckpointExample.zipWith.checkpoint
	|_ checkpoint ⇢ CheckpointExample.map.checkpoint
Original Stack Trace: ...
~~~

### checkPoint() 사용하기 : Traceback, Description 출력

checkpoint(description, forceStackTrace)를 사용하면 decription과 Traceback을 모두 출력할 수 있습니다.

~~~java
public class CheckPointExample{
    public static void main(String[] args) {
        Flux.just(2, 4, 6, 8)
            .zipWith(Flux.just(1, 2, 3, 0), (x, y) -> x / y)
            .checkpoint("CheckpointExample.zipWith.checkpoint", true)
            .map(num -> num + 2)
            .checkpoint("CheckpointExample.map.checkpoint", true)
            .subscribe(
                data -> log.info("# onNext: {}", data),
                error -> log.info("# onError: ", error)
            );

        Thread.sleep(100L);
    }
}
~~~
~~~
48:07 [main] - # onNext: 4
48:07 [main] - # onNext: 4
48:07 [main] - # onNext: 4
48:07 [main] - # onError: 
java.lang.ArithmeticException: / by zero
	at com.sim.webflux.WebfluxApplication.lambda$main$0(WebfluxApplication.java:29)
	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Assembly trace from producer [reactor.core.publisher.FluxZip], described as [CheckpointExample.zipWith.checkpoint] :
	reactor.core.publisher.Flux.checkpoint(Flux.java:3621)
	com.sim.webflux.WebfluxApplication.main(WebfluxApplication.java:30)
Error has been observed at the following site(s):
	*__checkpoint(CheckpointExample.zipWith.checkpoint) ⇢ at com.sim.webflux.WebfluxApplication.main(WebfluxApplication.java:30)
	|_     checkpoint(CheckpointExample.map.checkpoint) ⇢ at com.sim.webflux.WebfluxApplication.main(WebfluxApplication.java:32)
Original Stack Trace: ...
~~~


## log() 사용하기

log() Operator는 Reactor Sequence의 동작을 로그로 출력하는데, 이 로그를 통해 디버깅이 가능합니다.

~~~java
public class LogExample{
        public static Map<String, String> fruits = new HashMap<>();

        static {
        fruits.put("banana","바나나");
        fruits.put("apple","사과");
        fruits.put("pear","배");
        fruits.put("grape","포도");
    }

        public static void main(String[] args) {
        Flux.fromArray(new String[]{"banana","apple","pear","melon"})
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())
            .map(fruits::get)
            .map(fruit -> fruit + "가 나왔습니다.")
            .subscribe(log::info,
                error -> log.error("# onError: {}",error));

        Thread.sleep(100L);
    }
}
~~~
~~~
49:38 [main] - | onSubscribe([Fuseable] FluxPublishOn.PublishOnSubscriber)
49:38 [main] - | request(unbounded)
49:38 [parallel-1] - | onNext(banana)
49:38 [parallel-1] - 바나나가 나왔습니다.
49:38 [parallel-1] - | onNext(apple)
49:38 [parallel-1] - 사과가 나왔습니다.
49:38 [parallel-1] - | onNext(pear)
49:38 [parallel-1] - 배가 나왔습니다.
49:38 [parallel-1] - | onNext(melon)
49:38 [parallel-1] - | cancel() // 예외 발생!
49:38 [parallel-1] - # onError: {}
java.lang.NullPointerException: The mapper [com.sim.webflux.WebfluxApplication$$Lambda$202/0x000000700114dfc0] returned a null value
~~~




> 참고 :
>
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)