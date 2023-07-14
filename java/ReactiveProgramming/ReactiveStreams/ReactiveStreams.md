# Reactive Streams

## Reactive Streams란?

리엑티브 스트림즈는 개발자가 리액티브한 코드를 작성하기 위한 라이브러리의 표준 사양입니다.

리엑티브 스트림즈는 한마디로 **'데이터 스트림을 Non-Blocking이면서 비동기적인 방식으로 처리하기 위한 리액티브 라이브러리의 표준 사양'** 입니다.

이를 구현한 구현체로는 RxJava, Reactor, Akka Streams, Java 9 Flow API 등이 있습니다. 이중 Reactor는 Spring 진영에서 사용하는 구현체입니다.

## Reactive Streams 구성요소

| 컴포넌트         | 설명                                                                                                              |
|:-------------|:----------------------------------------------------------------------------------------------------------------|
| Publisher    | 데이터를 생성하고 발행하는 역할을 합니다.                                                                                         |
| Subscriber   | 구독한 Publisher로부터 발행된 데이터를 전달받아서 처리하는 역할을 합니다.                                                                   |
| Subscription | Publisher에 요청할 데이터의 개수를 지정하고, 데이터의 구독을 취소하는 역할을 합니다.                                                            |
| Processor    | Publisher와 Subscriber의 기능을 모두 가지고 있다. 즉, Subscriber로서 Publisher로부터 데이터를 받아 가공하고, Publisher로서 데이터를 발행하는 역할을 합니다. |

## Publisher

~~~java
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
~~~

Publisher는 subcribe 메소드를 통해서 Subscriber를 구독합니다. 이는 Observer Pattern에서의 Subject가 Observer를 구독하는 것과 유사합니다.

## Subscriber

~~~java
public interface Subscriber<T> {
    public void onSubscribe(Subscription s);

    public void onNext(T t);

    public void onError(Throwable t);

    public void onComplete();
}
~~~

Subscrirber 인터페이스는 4개의 메서드를 제공하고 있습니다.

* onSubscribe : Publisher을 구독하는 시점에 요청할 데이터의 개수를 지정하거나 후에 구독을 취소하는 역할을 합니다.
* onNext : Publisher로부터 수신한 데이터를 처리하는 역할을 합니다.
* onError : Publisher로부터 데이터를 수신하는 과정에서 에러가 발생했을 때 해당 에러를 처리하는 역할을 합니다.
* onComplete : Publisher가 데이터 발행을 완료했음을 알릴 때 호출되는 메서드입니다. 데이터 발행이 정상적으로 완료될 경우 후처리가 필요하다면 onComplete 메서드에서 처리 코드를 작성하면
  됩니다.

## Subscription

~~~java
public interface Subscription {
    public void request(long n);

    public void cancel();
}
~~~

Subscription 인터페이스는 Subscriber가 구독한 데이터의 개수를 지정하거나, 데이터 요청 취소하는 역할을 합니다.

## Processor

~~~java
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
~~~

Processor 인터페이스는 별도로 구현해야하는 메서드는 없습니다. Processor의 상속구조를 통해 Publisher와 Subscriber의 기능을 모두 가지고 있다는 것을 알 수 있습니다.

## Reactive Streams 관련 용어

* Signal : Publisher와 Subscriber간에 주고 받는 상호작용
* Demand : Publisher가 아직 전달하지 않은 Subscriber가 요청한 데이터
* Emit : Publisher가 Subscriber에게 데이터를 전달하는 것
* Upstream, Downstream : 데이터 스트림 관점에서 메서드 체이닝의 상위 메서드는 하위 메서드의 Upstream이 되고, 하위 메서드는 상위 메서드의 Downstream이 됩니다.
* Sequence : Publisher가 emit하는 데이터의 연속적인 흐름을 정의해 놓은 것
* Operator : stream에서 사용하는 filter, map, flapMap와 같은 메서드를 Operator라고 합니다.
* Source : 최초에 가장 먼저 생성된 데이터


