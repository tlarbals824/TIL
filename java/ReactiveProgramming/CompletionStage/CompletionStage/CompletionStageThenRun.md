# CompletionStage : thenRun[Async]

* Runnable을 파라미터로 받는다.
* 이전 task로부터 값을 받지 않고 값을 반환하지 않는다.
* 다음 task에게 null이 전달된다.
* future가 완료되었다는 이벤트를 기록할 때 유용

~~~java

@FunctionalInterface
public interface Runnable {
    public abstract void run();
}

public class CompletionStage {
    public CompletionStage<Void> thenRun(Runnable action);

    public CompletionStage<Void> thenRunAsync(Runnable action);
}
~~~

## thenRunAsync

~~~java
public class CompletionStageThenRunExample {
    public static void thenRun() {
        log.info("start main");
        var stage = Helper.completionStage();
        stage.thenRunAsync(() -> {
            log.info("in thenRunAsync");
        }).thenRunAsync(() -> {
            log.info("in thenRunAsync2");
        }).thenAcceptAsync(value -> {
            log.info("{} in thenAcceptAsync", value);
        });
        Thread.sleep(100);
    }
}
~~~

~~~
[           main] c.s.c.CompletionStageThenRunExample      : start main
[onPool-worker-1] com.sim.completionstage.Helper           : return in future
[onPool-worker-1] c.s.c.CompletionStageThenRunExample      : in thenRunAsync
[onPool-worker-1] c.s.c.CompletionStageThenRunExample      : in thenRunAsync2
[onPool-worker-1] c.s.c.CompletionStageThenRunExample      : null in thenAcceptAsync
~~~