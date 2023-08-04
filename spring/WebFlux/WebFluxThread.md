## Spring WebFlux의 스레드 모델

Spring MVC의 경우, 클라이언트의 요청이 들어올 때마다 서블릿 컨테이너의 스레드 풀에 미리 생성되어 있는 스레드가 요청을 처리하고 요청 처리를 완료하면 스레드 풀에 반납되는 스레드 모델을 사용합니다.

Spring WebFlux는 Non-Blocking I/O를 지원하는 Netty 등의 서버 엔진에서 적은 수의 고정된 크기의 스레드를 생성해서 대량의 요청을 처리합니다.

그런데 서버 측에서 복잡한 연산을 처리하는 등의 CPU 집약적인 작업을 처리하거나 요청중 Blocking 지점이 있다면 오히려 성능이 저하될 수 있습니다.

 이러한 성능 저하를 보완하고자 Reactor에서는 서버 엔진에서 제공하는 스레드 풀이 아닌 다른 스레드풀을 사용하는 [스케줄러(Scheduler)](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/Scheduler.md)를 제공합니다. 

### Reactor Netty 스레드

<img src="img/WebFluxApplicationOnNetty.jpg" width="500">

Reactor Netty의 LoopResources 인터페이스 코드의 일부입니다. 해당 코드에서 CPU 코어가 4개 이하면 4개의 워커 스레드를 생성하고, 4보다 많다면 코어 수만큼 스레드를 생성합니다.

~~~java
@FunctionalInterface
public interface LoopResources extends Disposable {
    int DEFAULT_IO_WORKER_COUNT = Integer.parseInt(System.getProperty(
        ReactorNetty.IO_WORKER_COUNT,
        "" + Math.max(Runtime.getRuntime().availableProcessors(), 4)));
    ...
}
~~~

### 워커 스레드 수 설정

Reactor Netty에서 성능 테스트등의 이유로 해당 워커 스레드의 수를 조절할 수 있습니다.
~~~
System.setProperty("reactor.netty.ioWorkerCount", "1");
~~~

> 참고 :
>
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)