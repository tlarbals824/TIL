package com.sim.completionstage;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

@Slf4j
public class CompletionStageThenAcceptAsyncExecutorExample {
    @SneakyThrows
    public static void thenAsync() {
        var single = Executors.newSingleThreadExecutor();
        var fixed = Executors.newFixedThreadPool(10);
        log.info("start main");
        CompletionStage<Integer> stage = Helper.completionStage();
        stage.thenAcceptAsync(i -> {
            log.info("{} in thenAcceptAsync", i);
        }, fixed).thenAcceptAsync(i -> {
            log.info("{} in thenAcceptAsync2", i);
        }, single);
        log.info("after thenAccept");
        Thread.sleep(200);
        single.shutdown();
        fixed.shutdown();
    }
}
