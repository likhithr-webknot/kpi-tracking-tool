package com.webknot.kpi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Task Configuration
 * 
 * Configures thread pool for async operations like notification dispatching.
 * This allows notification sending to happen in the background without holding
 * database connections, preventing connection pool exhaustion.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - always keep this many threads alive
        executor.setCorePoolSize(5);
        
        // Max pool size - can grow to this many threads
        executor.setMaxPoolSize(20);
        
        // Queue capacity - how many tasks to queue before rejecting
        executor.setQueueCapacity(100);
        
        // Thread name prefix for monitoring
        executor.setThreadNamePrefix("kpi-async-");
        
        // Wait for tasks to complete on shutdown (max 5 seconds instead of 30)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        
        // Rejection policy - what to do if queue is full
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}
