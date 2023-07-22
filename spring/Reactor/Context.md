# Context

Context는 어떠한 상황에서 그 상황을 처리하기 위해 필요한 정보라고 볼 수 있습니다. 프로그래밍에서 ApplicationContext나 SecurityContext의 개념과 비슷하게 Reactor의
Context도 어떠한 상황을 처리하거나 해결하기 위해 필요한 정보를 제공하는 어떤 것이라고 할 수 있습니다.

> A key/value store that is propagated between components such as operators via the context protocol. Contexts are ideal
> to transport orthogonal information such as tracing or security tokens.

Reactor의 Context는 Operator 같은 Reactor 구성요소 간에 전파되는 key/value 형태의 저장소라고 정의합니다. 이때 전파는 Downstream에서 Upstream으로 Context가
전파되어 Operator 체이닝에서 각 Operator가 해당 Context의 정보를 동일하게 이용할 수 있음을 의미합니다.

Reactor의 Context는 ThreadLocal과 유사한 면이 있습니다. 하지만 ThreadLocal은 하나의 요청(스레드)에서 데이터가 공유되는 반면 Context는 Subscriber의 구독을 기준으로 멀티
스레드 환경에서 데이터가 공유됩니다. 즉, 구독이 발생할 때마다 해당 구독과 연결된 하나의 Context가 생성됩니다.

## Context 데이터 접근

Context의 경우 데이터를 읽는 방식은 크게 두가지 입니다.

1. 원본 데이터 소스 레벨에서 읽는 방식
2. Operator 체인의 중간에서 읽는 방식

Context는 ContextView 인터페이스를 상속받는 구조를 가지고 있습니다. ContextView 는 Context의 데이터를 읽는 역할을 하는 인터페이스 입니다. 이를 상속하는 Context는
ContextView의 읽기에 데이터 쓰기 역할이 추가된 인터페이스입니다. 따라서 데이터를 읽는 과정에서는 ContextView를 사용하며 쓰는 과정에서는 Context를 사용함을 알아야합니다.

### Context 데이터 접근 : 원본 데이터 소스 레벨에서 읽는 방식

원본 데이터 소스 레벨에서 읽는 방식은 deferContextual()이라는 Operator를 사용합니다. deferContextual()은 defer() Operator와 같은 원리로 동작합니다. 즉,
Context에 저장된 데이터와 원본 데이터 소스의 처리를 지연시키는 역할을 합니다. 처리의 지연은 해당 Publisher가 emit하는 데이터가 구독하는 시점에 정해짐을 의미합니다.

~~~java
public class ContextDefferContextualExample {
    public static void main(String[] args) {
        Mono<String> mono = Mono.deferContextual(ctx -> Mono.just(ctx.get("time") + " " + LocalDateTime.now())) // 처리 지연 발생!
            .map(data -> "CurrentTime: " + data)
            .contextWrite(context -> context.put("time", "KOR"));

        mono.subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(2000L);

        mono.subscribe(data -> log.info("# onNext: {}", data));
    }
}
~~~

~~~
48:35 [main] - # onNext: CurrentTime: KOR 2023-07-22T14:48:35.293386
48:37 [main] - # onNext: CurrentTime: KOR 2023-07-22T14:48:37.304622 // 시간 갱신
~~~

### Context 데이터 접근 : Operator 체인의 중간에서 읽는 방식

Context에 저장된 데이터를 읽기 위해서는 ContextView API를 사용해야 합니다. Java의 Map API 사용법과 유사합니다.

| ContextView API                  | 설명                                                                              |
|:---------------------------------|:--------------------------------------------------------------------------------|
| get(key)                         | ContextView에서 key에 해당하는 value를 반환합니다.                                           |
| getOrEmpty(key)                  | ContextView에서 key에 해당하는 value를 Optional로 래핑해서 반환합니다.                            |
| getOrDefault(key, default value) | ContextView에서 key에 해당하는 value를 가져온다. key에 해당하는 value가 없으면 default value를 가져옵니다. |
| hasKey(key)                      | ContextView에서 특정 key가 존재하는지를 확인합니다.                                             |
| isEmpty()                        | Context가 비어있는지 확인합니다.                                                           |
| size()                           | Context 내에 있는 key/value의 개수를 반환합니다.                                             |

~~~java
public class ContextViewAPIExample {
    public static void main(String[] args) {
        final String key1 = "company";
        final String key2 = "firstName";
        final String key3 = "lastName";

        Mono.deferContextual(ctx ->
                Mono.just(ctx.get(key1) + ", " +
                    ctx.getOrEmpty(key2).orElse("no firstName") + " " +
                    ctx.getOrEmpty(key3).orElse("no lastName"))
            )
            .publishOn(Schedulers.parallel())
            .contextWrite(context -> context.put(key1, "apple"))
            .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(100L);
    }
}
~~~

~~~
05:44 [parallel-1] - # onNext: apple, no firstName no lastName
~~~

### Context 데이터 접근 : 데이터 쓰기

 Context에서 데이터를 contextWrite() Operator를 통해서 Context에 데이터를 씁니다. 또한 contextWrite() Operator는 입력과 반환을 Context인 Function 타입 인터페이스를 파라미터로 받아 데이터 쓰기를 처리합니다.

~~~java
public abstract class Mono<T> implements CorePublisher<T> {
    ...
    public final <V> Mono<T> contextWrite(Function<Context, ? extends Context> mapper) {
        return onAssembly(new MonoContextWrite<>(this, mapper));
    }
}
~~~

Context에 데이터를 쓰는 Context API입니다.

| Context API                         | 설명                                           |
|-------------------------------------|----------------------------------------------|
| put(key, value)                     | key/value 형태로 Context에 값을 씁니다.               |
| of(key1, value1, key2, value2, ...) | key/value 형태로 Context에 여러 개의 값을 씁니다.         |
| putAll(ContextView)                 | 현재 Context와 파라미터로 입력된 ContextView를 merge합니다. |
| delete(key)                         | Context에서 key에 해당하는 value를 삭제합니다.            |

~~~java
public class ContextAPIExample {
    public static void main(String[] args) {
        final String key1 = "company";
        final String key2 = "firstName";
        final String key3 = "lastName";

        Mono.deferContextual(ctx ->
                Mono.just(ctx.get(key1) + ", " + ctx.get(key2) + " " + ctx.get(key3))
            )
            .publishOn(Schedulers.parallel())
            .contextWrite(context ->
                context.putAll(Context.of(key2, "kyu min", key3, "sim").readOnly())) // 데이터 쓰기(putAll)
//                .contextWrite(context -> {
//                            ContextView view = Context.of(key2, "kyu min", key3, "sim");
//                            ContextView view = Context.of(key2, "kyu min", key3, "sim").readOnly();
//                            return context.putAll(view);
//                        }
//                )
            .contextWrite(context -> context.put(key1, "apple")) // 데이터 쓰기(put)
            .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(100L);
    }
}
~~~
~~~
54:18 [parallel-1] - # onNext: apple, kyu min sim
~~~


## Context 특징

* Context는 구독이 발생할 때마다 하나의 Context가 해당 구독에 연결됩니다.
* Context는 Operator 체인의 아래에서 위로 전파됩니다.
* 동일한 키에 대한 값을 중복해서 저장하면 Operator 체인에서 가장 위쪽에 위치한 contextWrite()이 저장한 값으로 덮어씁니다.
* Inner Sequence 내부에서는 외부 Context에 저장된 데이터를 읽을 수 있습니다. 하지만 그 반대는 불가능합니다.
* Context는 인증 정보 같은 직교성(독립성)을 가지는 정보를 전송하는 데 적합합니다.

### Context 특징 : 여러 Subscriber가 구독할 때

Context의 특징중 하나로 **구독이 발생할 때마다 해당하는 하나의 Context가 하나의 구독에 연결됩니다.** 즉, 구독이 다르면 context 또한 다릅니다.

~~~java
public class ContextMultiSubscribeExample {
    public static void main(String[] args) {
        final String key1 = "company";

        Mono<String> mono = Mono.deferContextual(ctx ->
                Mono.just("Company: " + ctx.get(key1))
            )
            .publishOn(Schedulers.parallel());

        mono.contextWrite(context -> context.put(key1, "apple"))
            .subscribe(data -> log.info("# subscribe1 onNext: {}", data));

        mono.contextWrite(context -> context.put(key1, "Microsoft"))
            .subscribe(data -> log.info("# subscribe2 onNext: {}", data));

        Thread.sleep(100L);
    }
}
~~~
~~~
10:37 [parallel-1] - # subscribe1 onNext: Company: apple
10:37 [parallel-2] - # subscribe2 onNext: Company: Microsoft
~~~

### Context 특징 : Context 전파 방향

**Context는 Operator 체인의 아래에서 위로 전파됩니다. 즉, Downstream에서 Upstream으로 전파됩니다. 또한, 동일한 키에 대해 값을 중복으로 설정하면 Operator 체인 가장 위쪽에 위치한 contextWrite()이 저장한 값으로 덮어씁니다.**

일반적으로 모든 Operator에서 Context에 저장된 데이터를 읽을 수 있도록 contextWrite()을 Operator 체인의 맨 마지막에 위치합니다.

~~~java
public class ContextMultiContextWriteExample {
    public static void main(String[] args) {
        final String key1 = "company";
        final String key2 = "name";

        Mono
            .deferContextual(ctx ->
                Mono.just(ctx.get(key1))
            )
            .publishOn(Schedulers.parallel())
            .contextWrite(context -> context.put(key2, "Bill"))
            .transformDeferredContextual((mono, ctx) ->
                mono.map(data -> data + " " + ctx.getOrDefault(key2, "Steve"))
            )
            .contextWrite(context -> context.put(key1, "Apple"))
            .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(100L);
    }
}
~~~
~~~
15:01 [parallel-1] - # onNext: Apple Steve
~~~

### Context 특징 : Inner Sequence 와 Outer Sequence 간의 Context 전파

 **Context의 Inner Sequence와 Outer Sequence간의 참조 방향은 Inner -> Outer로의 참조만 가능합니다. 그 반대는 불가능합니다.**

~~~java
public class ContextInnerContextExample {
    public static void main(String[] args) {
        final String key1 = "company";

        Mono.just("Steve")
//            .transformDeferredContextual((stringMono, ctx) -> ctx.get("role")) // Outer Sequence에서 Inner Sequence로의 Context 참조로 예외 발생
            .flatMap(name ->
                Mono.deferContextual(ctx ->
                    Mono.just(ctx.get(key1) + ", " + name) // Inner Sequence에서 Outer Sequence의 Context를 참조
                        .transformDeferredContextual((mono, innerCtx) ->
                            mono.map(data -> data + ", " + innerCtx.get("role")) // Inner Sequence Context
                        )
                        .contextWrite(context -> context.put("role", "CEO")) // Inner Sequence Context
                )
            )
            .publishOn(Schedulers.parallel())
            .contextWrite(context -> context.put(key1, "Apple"))
            .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(100L);
    }
}
~~~
~~~
Inner Sequence -> Outer Sequence 참조(예외 X): 24:30 [parallel-1] - # onNext: Apple, Steve, CEO

Outer Sequence -> Inner Sequence 참조(예외 O): Caused by: java.util.NoSuchElementException: Context does not contain key: role
~~~

### Context 특징 : Publisher 조합에서의 Context 전파

Reactor에서는 zip() Operator를 통해 여러 Publisher를 하나의 Publisher로 묶을 수 있습니다. 이때 Context의 경우 Publisher의 변환과정과는 상관없이 마지막 반환된 Publisher에 contextWrite()를 통해 데이터를 전파한다면 어느 Publsher든 Context의 데이터를 접글할 수 있습니다. 즉, **Context는 인증 정보 같은 직교성(독립성)을 가지는 정보를 전송하는 데 적합합니다.**


~~~java
public class ContextMultiPublisherExample {
    private static final String HEADER_AUTH_TOKEN = "authToken";

    @SneakyThrows
    public static void main(String[] args) {
        Mono<String> mono =
            postBook(Mono.just(
                new Book("abcd-1111-3533-2809",
                    "Reactor's Bible",
                    "Kevin"))
            )
                .contextWrite(Context.of(HEADER_AUTH_TOKEN, "1q2w3e4r!"));

        mono.subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(100L);
    }

    private static Mono<String> postBook(Mono<Book> book) {
        return Mono.zip(book, Mono.deferContextual(ctx -> Mono.just(ctx.get(HEADER_AUTH_TOKEN))))
            .flatMap(tuple -> {
                String response = "POST the book(" + tuple.getT1().getBookName() +
                    ", " + tuple.getT1().getAuthor() + "> with token: " + tuple.getT2();
                return Mono.just(response);
            });
    }

    @AllArgsConstructor
    @Data
    static class Book {
        private String isbn;
        private String bookName;
        private String author;
    }
}
~~~
~~~
36:24 [main] - # onNext: POST the book(Reactor'x Bible, Kevin> with token: 1q2w3e4r!
~~~