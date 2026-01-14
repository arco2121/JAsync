package com.arco2121.jasync.Async;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncIntervalT {

    public static AtomicInteger integer = new AtomicInteger(0);
    public final int intervalId;
    private volatile boolean running;
    private final Thread runner;

    public AsyncIntervalT(AsyncT<?> task, int delay) {
        this.intervalId = integer.incrementAndGet();
        this.running = false;
        this.runner = new Thread(() -> {
            while (running) {
                try {
                    Async.timeout(task, delay);
                } catch (InterruptedException e) {
                    running = false;
                    interrupt();
                }
            }
        });
    }

    void start() {
        running = true;
        runner.start();
    }

    void stop() {
        running = false;
    }

    void interrupt() {
        running = false;
        this.runner.interrupt();
    }
}
