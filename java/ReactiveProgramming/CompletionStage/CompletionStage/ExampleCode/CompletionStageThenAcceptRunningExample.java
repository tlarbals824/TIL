package com.sim.completionstage;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionStage;

@Slf4j
public class CompletionStageThenAcceptRunningExample {

    @SneakyThrows
    public static void thenAccept() {
        log.info("start main");
        CompletionStage<Integer> stage = Helper.runningStage();
        stage.thenAccept(i ->{
            log.info("{} in thenAccept", i);
        }).thenAccept(i ->{
            log.info("{} in thenAccept2", i);
        });
        Thread.sleep(2000);
    }

    @SneakyThrows
    public static void thenAcceptAsync() {
        log.info("start main");
        CompletionStage<Integer> stage = Helper.runningStage();
        stage.thenAcceptAsync(i ->{
            log.info("{} in thenAcceptAsync", i);
        }).thenAcceptAsync(i ->{
            log.info("{} in thenAcceptAsync", i);
        });
        Thread.sleep(2000);
    }
}
