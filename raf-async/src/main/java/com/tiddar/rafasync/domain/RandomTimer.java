package com.tiddar.rafasync.domain;


import lombok.extern.log4j.Log4j2;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Log4j2
public class RandomTimer {
    Thread worker;
    Random random = new Random();
    Condition wait;
    Lock lock;
    int max;
    int min;
    Task task;


    public RandomTimer() {
        this.lock = new ReentrantLock();
        this.wait = lock.newCondition();
        this.worker = new Thread(() -> System.out.println("初始化random timer成功"));
    }

    public void start() {
        this.worker.start();
    }

    public void reLoop() {

        lock.lock();
        try {
            wait.signalAll();
        }finally {
            lock.unlock();
        }
    }

    public void changeTo(String name,Task task, int max, int min) {
        log.info("定时器切换为:"+name);
        if (this.worker.isAlive() && !this.worker.isInterrupted()) {
            this.worker.interrupt();
        }
        this.task = task;
        this.max = max;
        this.min = min;
        this.worker = new Thread(() -> {
            lock.lock();
            while (!worker.isInterrupted()) {
                try {
                    if (!wait.await(random.nextInt(max - min) + min, TimeUnit.MILLISECONDS)) {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    //todo
                    break;
                }
            }
            lock.unlock();
        });
        this.worker.start();
    }

}

interface Task {
    void run();
}