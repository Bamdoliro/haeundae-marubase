package com.bamdoliro.maru.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * DB 일정에 맞춰 1회성 작업(1차 합격자 선발)을 예약하는 스케줄러.
 */
@Configuration
public class TaskSchedulerConfig {

    @Bean
    public TaskScheduler admissionTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("admission-schedule-");
        // 실행 중인 선발은 끝까지 기다리되, 아직 시각이 안 된 예약 작업은 종료 시 버린다.
        // (재시작하면 ApplicationReadyEvent에서 활성 일정 기준으로 다시 예약된다)
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return scheduler;
    }
}
