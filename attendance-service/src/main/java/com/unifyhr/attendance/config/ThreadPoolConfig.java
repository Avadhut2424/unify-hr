package com.unifyhr.attendance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

    @Value("${attendance.absentee-job.thread-pool-size:10}")
    private int threadPoolSize;

    @Bean(destroyMethod = "shutdown")
    public ExecutorService attendanceExecutorService() {
        AtomicInteger threadNumber = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("attendance-worker-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };

        return Executors.newFixedThreadPool(threadPoolSize, threadFactory);
    }
}
