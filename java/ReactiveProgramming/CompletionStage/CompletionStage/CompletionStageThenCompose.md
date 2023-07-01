# CompletionStage : thenCompose[Async]

* Function을 파라미터로 받는다.
* 이전 task로부터 T 타입의 값을 받아서 가공하고 U 타입의 CompletionStage를 반환한다.
* 반환한 CompletionStage가 done 상태가 되면 값을 다음 task에 전달한다.
* 다른 future를 반환해야하는 경우 유용

~~~java
public interface Function<T, R> {
    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }
}

public class CompletionStage {
    public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);

    public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn);
}
~~~

## thenComposeAsync

~~~java
public class CompletionStageThenComposeExample {
    public static void thenCompose() {
        log.info("start main");
        CompletionStage<Integer> stage = Helper.completionStage();
        stage.thenComposeAsync(value -> {
            var next = Helper.addOne(value);
            log.info("in thenComposeAsync: {}", next);
            return next;
        }).thenComposeAsync(value -> {
            var next = Helper.addResultPrefix(value);
            log.info("in thenComposeAsync2: {}", next);
            return next;
        }).thenAcceptAsync(value -> {
            log.info("{} in thenAcceptAsync", value);
        });
        Thread.sleep(1000);
    }
}
~~~
~~~
[           main] c.CompletionStageThenComposeAsyncExample : start main
[onPool-worker-1] com.sim.completionstage.Helper           : return in future
[onPool-worker-1] c.CompletionStageThenComposeAsyncExample : in thenComposeAsync: java.util.concurrent.CompletableFuture@5f21d3ac[Not completed]
[onPool-worker-1] c.CompletionStageThenComposeAsyncExample : in thenComposeAsync2: java.util.concurrent.CompletableFuture@4ac0973e[Not completed]
[onPool-worker-2] c.CompletionStageThenComposeAsyncExample : Result: 2 in thenAcceptAsync
~~~


