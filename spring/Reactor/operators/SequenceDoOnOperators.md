## Sequence의 내부 동작 확인을 위한 Operator

Reactor에는 Upstream Publisher에서 emit되는 데이터의 변경 없이 부수 효과만 수행하기 위한 Operator들이 있습니다. 이는 doOnXXX()로 시작하는 Operator들입니다.

doOnXXX() Operator는 Consumer 또는 Runnable 타입의 함수형 인터페이스를 파라미터로 가지기에 벌도의 리턴 값이 없습니다.
따라서 Upstream Publisher로부터 emit되는 데이터를 이용해 Upstream Publisher의 내부 동작을 엿볼 수 있으며 로그를 출력하는 등의 디버깅 용도로 많이 사용됩니다.
또한 데이터 emit 과정에서 error가 발생하면 해당 에러에 대한 알림을 전송하는 로직을 추가하는 등 부수 효과를 위한 다양한 로직을 적용할 수 있습니다.

|      Operator      | 설명                                                                                |
|:------------------:|:----------------------------------------------------------------------------------|
|  doOnSubscribe()   | Publisher가 구독 중일 때 트리거되는 동작을 추가할 수 있습니다.                                          |
|   doOnRequest()    | Publisher가 요청을 수신할 때 트리거되는 동작을 추가할 수 있습니다.                                        |
|     doOnNext()     | Publisher가 데이터를 emit할 때 트리거되는 동작을 추가할 수 있습니다.                                     |
|   doOnComplete()   | Publisher가 성공적으로 완료되었을 때 트리거되는 동작을 추가할 수 있습니다.                                    |
|    doOnError()     | Publisher가 에러가 발생한 상태로 종료되었을 때 트리거되는 동작을 추가할 수 있습니다.                              |
|    doOnCancel()    | Publisher가 취소되었을 때 트리거되는 동작을 추가할 수 있습니다.                                          |
|  doOnTerminate()   | Publisher가 성곡적으로 완료되었을 때 또는 에러가 발생한 상태로 종료되었을 때 트리거되는 동작을 추가할 수 있습니다.             |
|     doOnEach()     | Publisher가 데이터를 emit할 때, 성공적으로 완료되었을 때, 에러가 발생한 상태로 종료되었을 때 트리거되는 동작을 추가할 수 있습니다. |
|   doOnDiscard()    | Upstream에 있는 전체 Operator 체인의 동작 중에서 Operator에 의해 폐기 되는 요소를 조건부로 정리할 수 있습니다.       |
| doAfterTerminate() | Downstream을 성공적으로 완료한 직후 또는 에러가 발생하여 Publisher가 종료된 직후에 트리거되는 동작을 추가 할 수 있습니다.    |
|     doFirst()      | Publisher가 구독되기 전에 트리거되는 동작을 추가할 수 있습니다.                                          |
|    doFinally()     | 에러를 포함해서 어떤 이유이든 Publisher가 종료된 후 트리거되는 동작을 추가할 수 있습니다.                           |


~~~java
class DoOnOperators {
    public static void main(String[] args) {
        Flux.range(1,5)
            .doFinally(signalType -> log.info("# doFinally1: {}", signalType))
            .doFinally(signalType -> log.info("# doFinally2: {}", signalType))
            .doOnNext(data -> log.info("# range > doOnNext: {}", data))
            .doOnRequest(data -> log.info("# doOnRequest: {}", data))
            .doOnSubscribe(subscription -> log.info("# doOnSubscribe: {}", subscription))
            .doFirst(() -> log.info("# doFirst"))
            .filter(num -> num%2==1)
            .doOnNext(data -> log.info("# filter > doOnNext: {}", data))
            .doOnComplete(() -> log.info("# doOnComplete"))
            .subscribe(new BaseSubscriber<Integer>() {
                @Override
                protected void hookOnSubscribe(Subscription subscription) {
                    request(1);
                }

                @Override
                protected void hookOnNext(Integer value) {
                    log.info("# hookOnNext: {}", value);
                    request(1);
                }
            });
    }
}
~~~
~~~
19:54:26:89 [main] - # doFirst
19:54:26:90 [main] - # doOnSubscribe: reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber@12aba8be
19:54:26:90 [main] - # doOnRequest: 1
19:54:26:90 [main] - # range > doOnNext: 1
19:54:26:90 [main] - # filter > doOnNext: 1
19:54:26:90 [main] - # hookOnNext: 1
19:54:26:90 [main] - # doOnRequest: 1
19:54:26:90 [main] - # range > doOnNext: 2
19:54:26:90 [main] - # range > doOnNext: 3
19:54:26:90 [main] - # filter > doOnNext: 3
19:54:26:90 [main] - # hookOnNext: 3
19:54:26:90 [main] - # doOnRequest: 1
19:54:26:90 [main] - # range > doOnNext: 4
19:54:26:90 [main] - # range > doOnNext: 5
19:54:26:90 [main] - # filter > doOnNext: 5
19:54:26:90 [main] - # hookOnNext: 5
19:54:26:90 [main] - # doOnRequest: 1
19:54:26:90 [main] - # doOnComplete
19:54:26:90 [main] - # doFinally2: onComplete
19:54:26:90 [main] - # doFinally1: onComplete
~~~