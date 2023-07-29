# Testing

Reactor에서 가장 일반적인 테스트 방식은 Flux, Mono를 Reactor Sequence로 정의한 후, 구독 시점에 해당 Operator 체인이 시나리오대로 동작하는지를 테스트하는 것입니다.

이러한 테스트하기 위해서 Reactor에서는 StepVerifier를 제공합니다.

## StepVerifier

StepVerifer를 이용한 가장 기본적인 테스트 방식은 Reactor Sequence에서 발생하는 Signal 이벤트를 테스트하는 것입니다.

~~~java
class ReactorTestExample {
    @Test
    void syaHelloReactorTest() {
        StepVerifier
            .create(Mono.just("Hello Reactor"))
            .expectNext("Hello Reactor")
            .expectComplete()
            .verify();
    }
}
~~~

기본적인 사용방법은 다음과 같습니다.

1. create()를 통해 테스트 대상 Sequence를 생성합니다.
2. expectXXX()를 통해 Sequence에서 예상되는 Signal의 기댓값을 평가합니다.
3. verify()를 호출함으로써 전체 Operator 체인의 테스트를 트리거합니다.

### expectXXX() 메소드

| 메소드                                                 | 설명                                                             |
|-----------------------------------------------------|----------------------------------------------------------------|
| expectSubscription()                                | 구독이 이루어짐을 기대합니다.                                               |
| expectNext(T t)                                     | onNext Signal을 통해 전달되는 값이 파라미터로 전달된 값과 같음을 기대합니다.              |
| expectComplete()                                    | onComplete Signal이 전송되기를 기대합니다.                                |
| expectError()                                       | onError Signal이 전송되기를 기대합니다.                                   |
| expectNextCount(long count)                         | 구독 시점 또는 이전 expectNext()를 통해 기댓값이 평가된 데이터 이후부터 emit된 수를 기대합니다. |
| expectNoEvent(Duration duration)                    | 주어진 시간 동안 Signal 이벤트가 발생하지 않았음을 기대합니다.                         |
| expectAccessibleContext()                           | 구독 시점 이후에 Context가 전파되었음을 기대합니다.                               |
| expectNextSequence(Iterable<? extends T> iterables) | emit된 데이터들이 파라미터로 전달된 Iterable의 요소와 매치됨을 기대합니다.                |

### verifyXXX() 메소드

| 메소드                              | 설명                                                  |
|----------------------------------|-----------------------------------------------------|
| verify()                         | 검증을 트리거합니다.                                         |
| verifyComplete()                 | 검증을 트리거하고, onComplete Signal을 기대합니다.                |
| verifyError()                    | 검증을 트리거하고, onError Signal을 기대합니다.                   |
| verifyTimeout(Duration duration) | 검증을 트리거하고, 주어진 시간이 초과되어도 Publisher가 종료되지 않음을 기대합니다. |

## 시간 기반 테스트

StepVerifier는 가상 시간을 이용해 미래에 실행되는 Reactor Sequence의 시간을 앞당겨 테스트할 수 있는 기능을 지원합니다.

StepVerifer는 withVirtualTIme() 메소드를 제공합니다. 이는 VirtualTimeScheduler라는 가상 스케줄러의 제어를 받도록 해 줍니다. 따라서 구독에 대한 기댓값을 평가하고 난 후
then() 메서드를 사용해서 후속 작업을 할 수 있도록 합니다. 이때 VirtualTimeScheduler의 advanceTimeBy()를 이용하여 시간을 당기는 작업을 수행할 수 있습니다.

~~~java
class TimeBasedTest {
    @Test
    void getCOVID19CountTestOfHours() {
        StepVerifier
            .withVirtualTime(() -> TimeBasedTestExample.getCOVID19Count(Flux.interval(Duration.ofHours(1)).take(1)))
            .expectSubscription()
            .then(() -> VirtualTimeScheduler // 가상 스케줄러
                .get()
                .advanceTimeBy(Duration.ofHours(1))) // 1시간 앞당길 수 있습니다.
            .expectNextCount(11)
            .expectComplete()
            .verify();
    }
}
~~~

StepVerifer에서 withVirtualTime()과 expectNoEvent()를 같이 사용한다면 expectNoEvent에 명시한 시간만큼 앞당기며 테스트를 진행할 수 있습니다.

~~~java
class TimeBasedTest {
    @Test
    void getVoteCountTest() {
        StepVerifier
            .withVirtualTime(() -> TimeBasedTestExample.getVoteCount(Flux.interval(Duration.ofMinutes(1))))
            .expectSubscription()
            .expectNoEvent(Duration.ofMinutes(1)) // 1분동안 아무일도 발생하지 않음, 지정한 시간만큼 시간을 앞당김.
            .expectNoEvent(Duration.ofMinutes(1))
            .expectNoEvent(Duration.ofMinutes(1))
            .expectNoEvent(Duration.ofMinutes(1))
            .expectNoEvent(Duration.ofMinutes(1))
            .expectNextCount(5)
            .expectComplete()
            .verify();
    }
}
~~~

## Backpressure 테스트

StepVerifier를 사용하면 Backpressure에 대한 테스트를 진행할 수 있습니다.

Backpressure를 테스트할때, thenConsumeWhile로 emit되는 데이터를 소비하며 테스트를 할 수 있습니다. 또한 Backpressure 테스트에서 verifyThenAssertThat()을
이용하여 추가적인 테스트를 진행할 수 있습니다. 아래의 예로는 소비되는 데이터가 버려지는지 검증하는 hasDiscardedElements()를 사용하고 있습니다.

~~~java
class BackpressureTest {
    @Test
    void generateNumberPassTest() {
        StepVerifier
            .create(BackpressureTestExample.generateNumber(), 1L)
            .thenConsumeWhile(num -> num >= 1)
            .expectError()
            .verifyThenAssertThat()
            .hasDiscardedElements();
    }
}
~~~

## Context 테스트

Reactor Sequence에서 사용되는 Context는 StepVerifier를 통해 테스트할 수 있습니다.

Context에 대한 테스트는 expectAccessibleContext()를 통해 진행을 합니다. 이는 위에도 설명되어 있듯이 구독 시점 이후에 Context가 전파되었음을 기대합니다. 또한 hasKey()를
이용하여 해당 context에 key/value 값이 있는지 검증할 수 있습니다.

~~~java
class ContextTest {
    @Test
    void getSecretMessageTest() {
        Mono<String> source = Mono.just("hello");

        StepVerifier
            .create(
                ContextTestExample
                    .getSecretMessage(source)
                    .contextWrite(context ->
                        context.put("secretMessage", "Hello, Reactor"))
                    .contextWrite(context ->
                        context.put("secretKey", "aGVsbG8="))
            )
            .expectSubscription()
            .expectAccessibleContext() // context 검증 시작
            .hasKey("secretKey")    // context key/value 검증
            .hasKey("secretMessage") // context key/value 검증
            .then()
            .expectNext("Hello, Reactor")
            .expectComplete()
            .verify();
    }
}
~~~

## Record 테스트

Reactor Sequence를 테스트를 한다면 expectNext()로 emit된 데이터의 단순 기댓값만 평가하기보다 좀 더 구체적인 조건으로 Assertion해야할 경우가 있습니다.

이러한 경우 StepVerifier의 recordWith()를 통해 Java의 컬랙션에 emit된 데이터를 추가하고 세션을 시작하여 테스트를 할 수 있습니다.

마지막으로 consumeRecordedWith()를 통해 컬랙션에 기록된 데이터를 소비합니다. 이때 assertJ나 junit을 이용한 테스트를 진행할 수 있습니다.

~~~java
class RecordTest {
    @Test
    void getCityTest() {
        StepVerifier
            .create(RecordTestExample.getCapitalizedCountry(
                Flux.just("korea", "england", "canada", "india")
            ))
            .expectSubscription()
            .recordWith(ArrayList::new)
            .thenConsumeWhile(country -> !country.isEmpty())
            .consumeRecordedWith(countries -> {
                Assertions.assertThat(countries.stream()
                    .allMatch(country ->
                        Character.isUpperCase(country.charAt(0)))).isTrue();
            })
            .expectComplete()
            .verify();
    }
}
~~~

## TestPublisher

### 정상 동작하는 TestPublisher

TestPublisher를 사용하면, 개발자가 직접 프로그래밍 방식으로 Signal을 발생시키면서 원하는 상황을 미세하게 재연하며 테스트를 진행할 수 있습니다.

TestPublisher는 복잡한 로직이 포함된 대상 메서드를 테스트하거나 조건에 따라서 Signal을 변경해야 되는 등의 특정 상황을 테스트하기가 용이합니다.

> 정상 동작하는(Well-behaved) TestPublisher의 의미
>
> '정상 동작하는 TestPublisher'라는 말의 의미는 emit하는 데이터가 Null인지, 요청하는 개수보다 더 많은 데이터를 emit하는지 등의 리액티브 스트림즈 사용 위반 여부를 사전에 체크한다는
> 의미입니다.

~~~java
class Well_behavedTestPublisherTest {
    @Test
    void divideByTwoTestWithTestPublisher() {
        TestPublisher<Integer> source = TestPublisher.create();

        StepVerifier
            .create(GeneralTestExample.divideByTwo(source.flux()))
            .expectSubscription()
            .then(() -> source.emit(2, 4, 6, 8, 10))
            .expectNext(1, 2, 3, 4)
            .expectError()
            .verify();
    }
}
~~~

### 오동작하는 TestPublisher

오동작하는 TestPublisher를 생성하여 리액티브 스트림즈 사양을 위반하는지를 테스트할 수 있습니다.

> 오동작하는(Misbehaving) TestPublisher의 의미
>
> '오동작하는 TestPublisher'라는 말의 의미는 리액티브 스트림즈 사양 위반 여부를 사전에 체크하지 않는다는 의미입니다. 따라서 리액티브 스트림즈 사양에 위반되더라도 TestPublisher는 데이터를
> emit할 수 있습니다.

| 조건                   | 설명                                                                                |
|----------------------|-----------------------------------------------------------------------------------|
| ALLOW_NULL           | 전송할 수 있는 데이터가 null이여도 NullpointerException이 발생하지 않고 다음 호출을 진행할 수 있도록 합니다.         |
| CLEANUP_ON_TERMINATE | onComplete, onError, emit 같은 Terminal Signal을 연달아 여러 번 보낼 수 있도록 합니다.              |
| DEFER_CANCELLATION   | cancel Signal을 무시하고 계속해서 Signal을 emit할 수 있도록 합니다.                                 |
| REQUEST_OVERFLOW     | 요청 개수보다 더 많은 Signal이 발생하더라도 IllegalStateException이 발생시키지 않고 다음 호출을 진행할 수 있도록 합니다. |

~~~java
class MisbehavingTestPublisherTest {
    @Test
    void divideByTwoTestWithMisbehavingTestPublisher() {
        TestPublisher<Integer> source = TestPublisher.createNoncompliant(TestPublisher.Violation.ALLOW_NULL); // 조건 설정

        StepVerifier
            .create(GeneralTestExample.divideByTwo(source.flux()))
            .expectSubscription()
            .then(() -> {
                getDataSource().stream()
                    .forEach(data -> source.next(data));
                source.complete();
            })
            .expectNext(1, 2, 3, 4, 5)
            .expectComplete()
            .verify();
    }
}
~~~
~~~
java.lang.NullPointerException: e
	at java.base/java.util.Objects.requireNonNull(Objects.java:235)
	at reactor.util.concurrent.SpscArrayQueue.offer(SpscArrayQueue.java:51)
	at reactor.core.publisher.FluxZip$ZipInner.onNext(FluxZip.java:1114)
	at reactor.test.publisher.DefaultTestPublisher$TestPublisherSubscription.onNext(DefaultTestPublisher.java:234)
    ...
~~~

## PublisherProbe 테스트

reactor-test 모듈은 PublisherProbe를 이용하여 Sequence의 실행 경로를 테스트할 수 있습니다.

주로 조건에 따라 Sequence가 분기되는 경우, Sequence의 실행 경로를 추적해서 정상적으로 실행되었는지 테스트할 수 있습니다.

~~~java
class PublisherProbeTest{
    @Test
    void publisherProbeTest(){
        PublisherProbe<String> probe = PublisherProbe.of(PublisherProbeTestExample.supplyStandbyPower());

        StepVerifier
            .create(PublisherProbeTestExample
                .processTask(
                    PublisherProbeTestExample.supplyMainPower(),
                    probe.mono())
            )
            .expectNextCount(1)
            .verifyComplete();

        probe.assertWasSubscribed(); // probe Publisher가 구독됐는지 검증
        probe.assertWasRequested(); // probe Publisher가 요청됐는지 검증
        probe.assertWasNotCancelled(); // probe Publisher가 취소되지 않았는지 검증
    }
}
~~~