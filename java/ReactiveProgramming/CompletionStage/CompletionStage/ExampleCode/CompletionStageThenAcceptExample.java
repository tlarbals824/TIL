package com.sim.completionstage;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionStage;

@Slf4j
public class CompletionStageThenAcceptExample {

    @SneakyThrows
    public static void thenAccept() {
        log.info("start main");
        CompletionStage<Integer> stage = Helper.finishedStage();
        stage.thenAccept(i -> {
            log.info("{} in thenAccept", i);
        }).thenAccept(i -> {
            log.info("{} in thenAccept2", i);
        });
        log.info("after thenAccept");
        Thread.sleep(100);
    }

    @SneakyThrows
    public static void thenAcceptAsync() {
        log.info("start main");
        CompletionStage<Integer> stage = Helper.finishedStage();
        stage.thenAcceptAsync(i ->{
            log.info("{} in thenAcceptAsync", i);
        }).thenAcceptAsync(i ->{
            log.info("{} in thenAcceptAsync2", i);
        });
        log.info("after thenAccept");
        Thread.sleep(100);
    }
}
