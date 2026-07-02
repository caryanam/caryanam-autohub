package com.autohub.configuration;


import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;

/**
 * Dedicated thread pool for WhatsApp notification dispatch so that a slow/unresponsive
 * Meta API never starves the application's main request-handling threads.
 */
@Configuration
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "whatsAppTaskExecutor")
    public TaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("wa-async-");
        // Caller-runs prevents silent task drops under burst load (e.g. festival sale traffic)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("Uncaught async exception in method '{}' with params {}",
                        method.getName(), params, ex);
    }
}