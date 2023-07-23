# Reactor

> 공식 문서 : https://projectreactor.io

## Reactor 목차

> [1. Marble Diagram](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/MarbleDiagram.md)
> 
> [2. Cold Sequence, Hot Sequence](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/ColdSequenceHotSequence.md)
> 
> [3. Backpressure](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/Backpressure.md)
> 
> [4. Sinks](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/Sinks.md)
> 
> [5. Scheduler](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/Scheduler.md)
> 
> [6. Context](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/Context.md)
> 
> [7. Debugging](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/Debugging.md)
> 
> [8. Testing(정리중)](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/Testing.md)
> 
> [9. Operators(정리중)](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/Operators.md)


## Reactor의 특징

1. Reactive Streams 구현한 리액티브 라이브러리
2. Non-Blocking
3. Java's Functional Interface
4. Flux[N], Mono[0|1]
5. Well-suited for microservices
6. Backpressure-ready network

## Reactor의 구성요소

Reactor의 Flux을 이용하여 예시를 구성해보면 다음과 같습니다.

~~~java
public class FluxExample{
    public static void main(String[] args) {
        Flux<String> sequence = Flux.just("Hello", "Reactor");
        sequence.map(data -> data.toLowerCase())
                .subscribe(data -> System.out.println(data));
    }
}
~~~

우선 Flux는 Reactor에서 Publisher의 역할을 합니다. Reactive Streams에서 Publisher는 입력 데이터에 대해 제공하는 역할을 한다고 나와있습니다. 따라서 Flux의 just(...)는 Publisher의 입력 데이터라고 할 수 있습니다. 이 가공되지 않은 데이터로서 데이터 소스라고 불립니다.

 그 다음 .subscribe(...) 메소드의 파라미터로 전달된 람다식이 Flux(Publisher)에 대한 Subscriber 역할을 합니다.

 마지막으로 .map(...) 메소드는 Operator로서 상위에서 전달 받은 데이터를 가공하는 역할을 합니다. 또한 이러한 Operator는 Flux나 Mono로 반환함으로써 메서드 체이닝을 구성하여 연속적으로 Operator를 구성할 수 있습니다.

 Reactor의 흐름을 정리하면 다음과 같습니다.
1. 데이터를 생성해서 제공(**Flux.just**)
2. 데이터를 가공(**.map**)
3. 전달받은 데이터를 처리한다(**.subscribe**)

> 참고 :
>
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)