package com.tiddar.raf.domain;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.log4j.Log4j2;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Log4j2
public class RandomTimer {
    Thread supervisor;
    Random random = new Random();
    Condition wait;
    Lock lock;
    int max;
    int min;
    Runnable task;
    ExecutorService worker;
    String name;


    public RandomTimer() {
        this.lock = new ReentrantLock();
        this.wait = lock.newCondition();
        this.name = "init";
        worker = Executors.newFixedThreadPool(16,
                new ThreadFactoryBuilder().setNameFormat("random timer  -%d").build());
        this.supervisor = new Thread(() -> System.out.println("初始化random timer成功"));
    }

    public void start() {
        this.supervisor.start();
    }

    public void reLoop() {

        lock.lock();
        try {
            wait.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void changeTo(String name, Runnable task, int max, int min) {
        this.name = name;
        log.info("定时器切换为:" + name);
        if (this.supervisor.isAlive() && !this.supervisor.isInterrupted()) {
            this.supervisor.interrupt();
        }
        this.task = task;
        this.max = max;
        this.min = min;
        this.supervisor = new Thread(() -> {
            lock.lock();
            while (!supervisor.isInterrupted()) {
                try {
                    if (!wait.await(random.nextInt(max - min) + min, TimeUnit.MILLISECONDS)) {
                        worker.execute(task);
                    }
                } catch (InterruptedException e) {
                    //todo
                    break;
                }
            }
            lock.unlock();
        });
        this.supervisor.start();
    }

}

