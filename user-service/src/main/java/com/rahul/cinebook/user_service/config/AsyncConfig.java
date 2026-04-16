package com.rahul.cinebook.user_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 1. Core Pool Size: Threads that stay alive even if idle
        executor.setCorePoolSize(5);

        // 2. Max Pool Size: Max threads allowed under heavy load
        executor.setMaxPoolSize(10);

        // 3. Queue Capacity: How many tasks wait before a new thread is created
        executor.setQueueCapacity(50);

        executor.setThreadNamePrefix("NPCine-Async-");
        executor.initialize();
        return executor;
    }

    /**
     * Production Fix: Since @Async methods return void, standard try-catch
     * in the controller won't work. This logs hidden background errors.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, obj) -> {
            log.error("Exception in async method: {} - {}", method.getName(), throwable.getMessage());
        };
    }
}