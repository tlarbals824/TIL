package com.sim.completionstage;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionStage;

@Slf4j
public class CompletionStageThenComposeExample {

    @SneakyThrows
    public static void thenCompose(){
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
