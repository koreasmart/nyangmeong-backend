package com.nyangmeong.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * [비동기 실행 설정]
 * - characterTaskExecutor : 픽셀 캐릭터 백그라운드 생성 (FN-MYP-010-09)
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    public static final String CHARACTER_EXECUTOR = "characterTaskExecutor";

    /**
     * 캐릭터 생성 스레드 풀
     */
    @Bean(name = CHARACTER_EXECUTOR)
    public Executor characterTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("character-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
