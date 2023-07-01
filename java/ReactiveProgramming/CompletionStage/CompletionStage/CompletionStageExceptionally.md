# CompletionStage : exceptionally

* Function을 파라미터로 받는다.
* 이전 task에서 발생한 exception을 받아서 처리하고 값을 반환한다.
* 다음 task에게 반환된 값이 전달된다.
* future 파이프에서 발생한 에러를 처리할때 유용

~~~java
public class CompletionStage {
    public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn);
}
~~~

## exceptionally

~~~java
public class CompletionStageExceptionallyExample {
    public static void exceptionally() {
        log.info("start main");
        var stage = Helper.completionStage();
        stage.thenApplyAsync(i -> {
            log.info("in thenApplyAsync");
            return i/0;
        }).exceptionally(e -> {
            log.info("{} in exceptionally", e.getMessage());
            return 0;
        }).thenAcceptAsync(value -> {
            log.info("{} in thenAcceptAsync", value);
        });
        Thread.sleep(1000);
    }
}
~~~
~~~
[onPool-worker-1] com.sim.completionstage.Helper           : return in future
[onPool-worker-1] .s.c.CompletionStageExceptionallyExample : in thenApplyAsync
[onPool-worker-1] .s.c.CompletionStageExceptionallyExample : java.lang.ArithmeticException: / by zero in exceptionally
[onPool-worker-2] .s.c.CompletionStageExceptionallyExample : 0 in thenAcceptAsync
~~~
