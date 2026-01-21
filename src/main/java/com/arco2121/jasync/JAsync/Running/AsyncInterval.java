package com.arco2121.jasync.JAsync.Running;

import com.arco2121.jasync.JAsync.Async;

import java.util.concurrent.atomic.AtomicInteger;

public final class AsyncInterval {

    private static final AtomicInteger integer = new AtomicInteger(0);
    private final int intervalId;
    private volatile boolean running;
    private final Thread runner;
    public int delay;

    public AsyncInterval(Runnable task, int delay) {
        this.intervalId = integer.incrementAndGet();
        this.running = false;
        this.delay = delay;
        this.runner = new Thread(() -> {
            while (running) {
                try {
                    Async.timeout(task, this.delay);
                } catch (InterruptedException e) {
                    running = false;
                    interrupt();
                }
            }
        });
    }

    public void start() {
        if(!running) {
            running = true;
            runner.start();
        }
    }

    public void stop() {
        if(running) running = false;
    }

    public void interrupt() {
        if(running) {
            running = false;
            this.runner.interrupt();
        }
    }

    public int getIntervalId() { return this.intervalId; }
}
