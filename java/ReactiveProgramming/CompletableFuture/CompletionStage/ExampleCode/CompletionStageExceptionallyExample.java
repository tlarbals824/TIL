package com.sim.completionstage;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class CompletionStageExceptionallyExample {
    @SneakyThrows
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
