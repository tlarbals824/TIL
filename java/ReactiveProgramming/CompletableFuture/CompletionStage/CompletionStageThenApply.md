# CompletionStage : thenApply[Async]

* Function을 파라미터로 받는다.
* 이전 task로부터 T 타입의 값을 받아서 가공하고 U 타입의 값을 반환한다.
* 다음 task에게 반환했던 값이 전달된다.
* 값을 변형해서 전달해야 하는 경우 유용

~~~java
public class CompletionStage {
    public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn);

    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn);
}
~~~

## thenApplyAsync

~~~java
public class CompletionStageThenApplyExample {
    public static void thenApplyAsync() {log.info("start main");
        CompletionStage<Integer> stage = Helper.completionStage();
        stage.thenApplyAsync(value -> {
            var next = value + 1;
            log.info("in thenApplyAsync: {}", next);
            return next;
        }).thenApplyAsync(value -> {
            var next = "result: " + value;
            log.info("in thenApplyAsync2: {}", next);
            return next;
        }).thenApplyAsync(value -> {
            boolean next = value.equals("result: 2");
            log.info("in thenApplyAsync3: {}", next);
            return next;
        }).thenAcceptAsync(value -> {
            log.info("{}", value);
        });
        Thread.sleep(100);
    }
}
~~~
~~~
[           main] c.s.c.CompletionStageThenApplyExample    : start main
[onPool-worker-1] com.sim.completionstage.Helper           : return in future
[onPool-worker-1] c.s.c.CompletionStageThenApplyExample    : in thenApplyAsync: 2
[onPool-worker-1] c.s.c.CompletionStageThenApplyExample    : in thenApplyAsync2: result: 2
[onPool-worker-1] c.s.c.CompletionStageThenApplyExample    : in thenApplyAsync3: true
[onPool-worker-1] c.s.c.CompletionStageThenApplyExample    : true
~~~
