package com.sim.completionstage;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionStage;

@Slf4j
public class CompletionStageThenApplyExample {
    @SneakyThrows
    public static void thenApply() {
        log.info("start main");
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
