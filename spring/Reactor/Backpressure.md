# Backpressure

Backpressure는 간단하게 설명하면, Publisher에서 Subscriber로 데이터가 emit 되는 상황에서 Subscriber의 처리속도가 느려 emit된 데이터가 대기 중이며 대기 중인 데이터가
지속적으로 쌓일때의 해결방법입니다. 즉 처리되지 않고 쌓이는 데이터의 처리방법이다.

Reactor에서 지원하는 Backpressure 처리 방식에는 크게 2개로 나눌 수 있습니다.

1. 데이터 요청 개수 제어 : Subscriber가 Publisher에게 요청할 데이터의 개수를 지정하여 Subscriber가 적절하게 처리할 수 있게하는 방법
2. Backpressure : Subscriber가 처리하지 못한 대기 중인 데이터의 처리 방법 (ex: 대기 중인 데이터 폐기, 버퍼에 존재하는 데이터 폐기)

## 데이터 요청 개수 제어

첫번째 방법인 데이터 요청 개수 제어는 Subscriber가 적절히 처리할 수 있는 수준의 데이터 개수를 Publisher에게 요청하는 것입니다.
이는 Subscriber가 request() 메서드를 통해 적절한 데이터 개수를 요청하는 방식입니다.

~~~java
public class BaseSubscriberBackpressureExample {
    public static void main(String[] args) {
        Flux.range(1, 10)
                .doOnRequest(data -> log.info("# doOnRequest: {}", data))
                .subscribe(new BaseSubscriber<Integer>() {

                    /**
                     * Subscriber 인터페이스에 정의된 onSubscribe() 메서드를 대신해
                     * 구독 시점에 request() 메서드를 호출해서 최초 데이터 요청 개수를 제어하는 역할을 합니다.
                     */
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1); // 최초 데이터 요청 개수 제어
                    }

                    /**
                     * Subscriber 인터페이스에 정의된 onNext() 메서드 대신해
                     * Publisher가 emit한 데이터를 전달받아 처리한 후에 Publisher에게 또다시 데이터를 요청하는 역할을 합니다.
                     * 이 역시 request() 메서드를 호출해서 데이터 요청 개수를 제어합니다.
                     */
                    @Override
                    protected void hookOnNext(Integer value) {
                        Thread.sleep(500L);
                        log.info("# hookOnNext: {}", value);
                        request(2); // 다시 데이터를 요청하는 데이터 개수 제어
                    }
                });
    }
}
~~~

~~~
14:19 [main] - # doOnRequest: 1 // 최초 데이터 요청 개수
14:20 [main] - # hookOnNext: 1
14:20 [main] - # doOnRequest: 2 // 다시 요청하는 데이터 개수
14:20 [main] - # hookOnNext: 2
14:20 [main] - # doOnRequest: 2 // 다시 요청하는 데이터 개수
14:21 [main] - # hookOnNext: 3
14:21 [main] - # doOnRequest: 2 // 다시 요청하는 데이터 개수
14:21 [main] - # hookOnNext: 4
14:21 [main] - # doOnRequest: 2 // 다시 요청하는 데이터 개수
...
~~~

## Backpressure 전략

두번째 방법으로는 Reactor에서 제공하는 Backpressure 전략을 사용하는 것입니다. Reactor는 Backpressure를 위한 다양한 전략을 제공합니다.

| 종류     | 설명                                                                                |
|:-------|:----------------------------------------------------------------------------------|
| IGNORE | Backpressure를 적용하지 않는다.                                                           |
| ERROR  | Downstream으로 전달할 데이터가 버퍼에 가득 찰 경우, Exception을 발생시키는 전략                            |
| DROP   | Downstream으로 전달할 데이터가 버퍼에 가득 찰 경우, 버퍼 밖에서 대기하는 먼저 emit된 데이터부터 Drop시키는 전략          |
| LATEST | Downstream으로 전달할 데이터가 버퍼에 가득 찰 경우, 버퍼 밖에서 대기하는 가장 최근에(나중에) emit된 데이터부터 버퍼에 채우는 전략 |
| BUFFER | Downstream으로 전달할 데이터가 버퍼에 가득 찰 경우, 버퍼 안에 있는 데이터부터 Drop시키는 전략                      |

### IGNORE 

IGNORE 전략은 말 그대로 Backpressure를 적용하지 않는 전략입니다.
IGNORE 전략을 사용할 경우 Downstream에서의 Backpressure 요청이 무시되기 때문에 IllegalStateException이 발생할 수 있습니다.

### ERROR

ERROR 전략은 Downstream의 데이터 처리 속도가 느려서 Upstream의 emit 속도를 따라가지 못할 경우 IllegalStateException을 발생시킵니다.

이 경우 Publisher는 Error Signal을 Subscriber에게 전송하고 삭제한 데이터는 폐기합니다.

~~~java
public class ErrorBackpressureExample {
    public static void main(String[] args) {
        Flux.interval(Duration.ofMillis(1L))
                .onBackpressureError() // ERROR 전략 적용
                .doOnNext(data -> log.info("# doOnNext: {}", data))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> {
                    try {
                        Thread.sleep(5L);
                    } catch (InterruptedException e) {
                    }
                    log.info("# onNext: {}", data);
                }, error -> log.error("# onError"));

        Thread.sleep(2000L);
    }
}
~~~

~~~
10:39 [parallel-2] - # doOnNext: 0
10:39 [parallel-2] - # doOnNext: 1
10:39 [parallel-2] - # doOnNext: 2
10:39 [parallel-2] - # doOnNext: 3
10:39 [parallel-2] - # doOnNext: 4
10:39 [parallel-2] - # doOnNext: 5
10:39 [parallel-2] - # doOnNext: 6
10:39 [parallel-2] - # doOnNext: 7
10:39 [parallel-1] - # onNext: 0
10:39 [parallel-2] - # doOnNext: 8
10:39 [parallel-2] - # doOnNext: 9
...
10:40 [parallel-1] - # onNext: 254
10:40 [parallel-1] - # onNext: 255
10:40 [parallel-1] - # onError      // Error 발생
~~~

### DROP

DROP 전략은 Publisher가 Downstream으로 전달할 데이터가 버퍼에 가득 찰 경우, 버퍼 밖에서 대기 중인 데이터 중에서 먼저 emit된 데이터부터 Drop 시키는 전략입니다. Drop된 데이터는 폐기됩니다.

ERROR 전략과 달린 DROP은 onBackpressureDrop() 메서드를 통해서 Drop된 데이터를 파라미터로 받을 수 있습니다. 이를통해 Drop된 데이터가 폐기되기 전에 추가적인 작업을 수행할 수 있습니다.

~~~java
public class DropBackpressureExample {
    public static void main(String[] args) {
        Flux.interval(Duration.ofMillis(1L))
                .onBackpressureDrop(dropped -> log.info("# dropped: {}", dropped)) // drop 전략 설정, drop된 데이터로 추가적인 작업을 수행할 수 있음
                .publishOn(Schedulers.parallel())
                .subscribe(data -> {
                    try {
                        Thread.sleep(5L);
                    } catch (InterruptedException e) {
                    }
                    log.info("# onNext: {}", data);
                }, error -> log.error("# onError"));

        Thread.sleep(2000L);
    }
}
~~~

~~~
21:21 [parallel-1] - # onNext: 0
21:21 [parallel-1] - # onNext: 1
21:21 [parallel-1] - # onNext: 2
21:21 [parallel-1] - # onNext: 3
...
21:22 [parallel-1] - # onNext: 39
21:22 [parallel-1] - # onNext: 40
21:22 [parallel-2] - # dropped: 256 // drop
21:22 [parallel-2] - # dropped: 257 // drop
...
21:23 [parallel-2] - # dropped: 1166 // drop
21:23 [parallel-2] - # dropped: 1167 // drop
21:23 [parallel-1] - # onNext: 191
21:23 [parallel-1] - # onNext: 192
...
~~~

### LATEST

LATEST 전략은 Publisher가 Downstream으로 전달할 데이터가 버퍼에 가득찰 경우, 버퍼 밖에서 대기 중인 데이터 중에서 가장 최근에(나중에) emit된 데이터부터 버퍼에 채우는 전략입니다.

Drop 전략은 버퍼가 가득 찰 경우 버퍼 밖에서 대기 중인 데이터를 하나씩 차례대로 Drop하면서 폐기합니다.

이와 달리 LATEST 전략은 **새로운 데이터가 들어오는 시점에 가장 최근 데이터만 남겨 두고 나머지 데이터를 폐기합니다.**

~~~java
public class LatestBackpressureExample {
    public static void main(String[] args) {
        Flux.interval(Duration.ofMillis(1L))
                .onBackpressureLatest() // Latest 전략 설정
                .publishOn(Schedulers.parallel())
                .subscribe(data -> {
                    try {
                        Thread.sleep(5L);
                    } catch (InterruptedException e) {
                    }
                    log.info("# onNext: {}", data);
                }, error -> log.error("# onError"));

        Thread.sleep(3000L);
    }
}
~~~

~~~
27:22 [parallel-1] - # onNext: 0
27:22 [parallel-1] - # onNext: 1
27:22 [parallel-1] - # onNext: 2
27:22 [parallel-1] - # onNext: 3
...
27:23 [parallel-1] - # onNext: 254
27:23 [parallel-1] - # onNext: 255 
27:23 [parallel-1] - # onNext: 1169 // 1169 데이터는 가장 최근에 emit 데이터이며 256~1168 데이터는 폐기됩니다.
27:23 [parallel-1] - # onNext: 1170
~~~

### BUFFER

컴퓨터 시스템에서 사용되는 버퍼의 일반적인 기능은 입출력을 수행하는 장치들간의 속도 차이를 조절하기 위해 입출력 장치 중간에 위치해서 데이터를 어느 정도 쌓아 두었다가 전송하는 것입니다.

Backpressure BUFFER 전략도 이와 비슷한 기능을 합니다. Backpressure BUFFER 전략은 버퍼의 데이터를 폐기하지 않고 버퍼링을 하는 전략도 지원하지만, 버퍼가 가득 차면 버퍼 내의 데이터를 폐기하는 전략, 그리고 버퍼가 가득차면 에러를 발생시키는 전략도 지원합니다.

 다음은 BUFFER에서 데이터를 폐기하는 전략을 설명합니다. 참고로 BUFFER에서의 데이터 폐기는 DROP과 LATEST 전략에서의 버퍼 외부의 대기 데이터의 폐기와는 다르게 버퍼 내부의 데이터를 폐기합니다.

#### DROP_LATEST

BUFFER DROP_LATEST 전략은 Publisher가 Downstream으로 전달할 데이터가 버퍼에 가득 찰 경우, 가장 최근에(나중에) 버퍼 안에 채워진 데이터를 Drop하여 페기한 후, 확보된 공간에 emit된 데이터를 채우는 전략입니다.

~~~java
public class BufferBackpressureExample_DropLatest {
    public static void main(String[] args) {
        Flux.interval(Duration.ofMillis(300L))
                .doOnNext(data -> log.info("# emitted by original Flux: {}", data))
                .onBackpressureBuffer(2,        // BUFFER 전략 설정 및 버퍼 크기 설정(2)
                        dropped -> log.info("** Overflow & Dropped: {} **", dropped), // drop된 데이터에 대한 추가적인 작업
                        BufferOverflowStrategy.DROP_LATEST) // DROP_LATEST 전략 설정
                .doOnNext(data -> log.info("[ # emitted by Buffer: {} ]", data))
                .publishOn(Schedulers.parallel(), false, 1)
                .subscribe(data -> {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                    }
                    log.info("# onNext: {}", data);
                }, error -> log.error("# onError"));

        Thread.sleep(3000L);
    }
}
~~~

~~~
33:00 [parallel-2] - # emitted by original Flux: 0 // 버퍼 [0]
33:00 [parallel-2] - [ # emitted by Buffer: 0 ] // 현재 처리중인 데이터 [0]
33:00 [parallel-2] - # emitted by original Flux: 1 // 버퍼 [1]
33:01 [parallel-2] - # emitted by original Flux: 2 // 버퍼 [2,1]
33:01 [parallel-2] - # emitted by original Flux: 3 // 버퍼 [2,1] 
33:01 [parallel-2] - ** Overflow & Dropped: 3 ** // Overflow 발생 및 3번 데이터 drop
33:01 [parallel-1] - # onNext: 0 // 0번 데이터 처리 완료
33:01 [parallel-1] - [ # emitted by Buffer: 1 ] // 현재 처리중인 데이터 [1]
33:01 [parallel-2] - # emitted by original Flux: 4 // 버퍼 [4,2]
33:02 [parallel-2] - # emitted by original Flux: 5 // 버퍼 [4,2]
33:02 [parallel-2] - ** Overflow & Dropped: 5 ** // Overflow 발생 및 5번 데이터 drop
33:02 [parallel-2] - # emitted by original Flux: 6 // 버퍼 [4,2]
33:02 [parallel-2] - ** Overflow & Dropped: 6 ** // Overflow 발생 및 6번 데이터 drop
33:02 [parallel-1] - # onNext: 1 // 1번 데이터 처리 완료
33:02 [parallel-1] - [ # emitted by Buffer: 2 ] // 현재 처리중인 데이터 [2]
33:02 [parallel-2] - # emitted by original Flux: 7 // 버퍼 [7,4]
~~~

#### DROP_OLDEST

BUFFER DROP_OLDEST 전략은 Publisher가 Downstream으로 전달할 데이터가 버퍼에 가득 찰 경우, 버퍼 안에 채워진 데이터 중에서 가장 오래된 데이터를 Drop하여 폐기한 후, 확보된 공간에 emit된 데이터를 채우는 전략입니다. 즉, BUFFER DROP_LATEST와 정반대되는 전략이라고 생각하면 됩니다.

~~~java
public class BufferBackpressureExample_DropOldest {
    public static void main(String[] args) {
        Flux.interval(Duration.ofMillis(300L))
                .doOnNext(data -> log.info("# emitted by original Flux: {}", data))
                .onBackpressureBuffer(2,    // BUFFER 전략 설정 및 버퍼 크기 설정(2)
                        dropped -> log.info("** Overflow & Dropped: {} **", dropped),   // drop된 데이터에 대한 추가적인 작업
                        BufferOverflowStrategy.DROP_OLDEST) // DROP_OLDEST 전략 설정
                .doOnNext(data -> log.info("[ # emitted by Buffer: {} ]", data))
                .publishOn(Schedulers.parallel(), false, 1)
                .subscribe(data -> {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                    }
                    log.info("# onNext: {}", data);
                }, error -> log.error("# onError"));

        Thread.sleep(3000L);
    }
}
~~~

~~~
38:21 [parallel-2] - # emitted by original Flux: 0 // 버퍼 [0]
38:21 [parallel-2] - [ # emitted by Buffer: 0 ] // 현재 처리중인 데이터 [0]
38:21 [parallel-2] - # emitted by original Flux: 1 // 버퍼 [1]
38:21 [parallel-2] - # emitted by original Flux: 2 // 버퍼 [2,1]
38:22 [parallel-2] - # emitted by original Flux: 3 // 버퍼 [3,2]
38:22 [parallel-2] - ** Overflow & Dropped: 1 ** // Overflow 발생 및 1번 데이터 drop
38:22 [parallel-1] - # onNext: 0 // 0번 데이터 처리 완료
38:22 [parallel-1] - [ # emitted by Buffer: 2 ] // 현재 처리중인 데이터 [2]
38:22 [parallel-2] - # emitted by original Flux: 4 // 버퍼 [4,3]
38:22 [parallel-2] - # emitted by original Flux: 5 // 버퍼 [5,4]
38:22 [parallel-2] - ** Overflow & Dropped: 3 ** // Overflow 발생 및 3번 데이터 drop
38:23 [parallel-2] - # emitted by original Flux: 6 // 버퍼 [6,5]
38:23 [parallel-2] - ** Overflow & Dropped: 4 ** // Overflow 발생 및 4번 데이터 drop
38:23 [parallel-1] - # onNext: 2 // 2번 데이터 처리 완료
38:23 [parallel-1] - [ # emitted by Buffer: 5 ] // 현재 처리중인 데이터 [5]
38:23 [parallel-2] - # emitted by original Flux: 7 // 버퍼 [7,6]
~~~

> 참고 : 
> 
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)