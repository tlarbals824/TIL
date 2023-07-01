package com.sim.completionstage;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
public class Helper {
    @SneakyThrows
    public static CompletionStage<Integer> completionStage() {
        var future = CompletableFuture.supplyAsync(() -> {
            log.info("return in future");
            return 1;
        });
        Thread.sleep(100);
        return future;
    }

    @SneakyThrows
    public static CompletionStage<Integer> finishedStage() {
        var future = CompletableFuture.supplyAsync(() -> {
            log.info("supplyAsync");
            return 1;
        });
        Thread.sleep(100);
        return future;
    }

    public static CompletionStage<Integer> runningStage() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                log.info("I'm running!");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 1;
        });
    }

    public static CompletionStage<Integer> addOne(int value){
        return CompletableFuture.supplyAsync(() -> {
            try{
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return value + 1;
        });
    }

    public static CompletionStage<String> addResultPrefix(int value){
        return CompletableFuture.supplyAsync(() -> {
            try{
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "Result: " + value;
        });
    }
}
