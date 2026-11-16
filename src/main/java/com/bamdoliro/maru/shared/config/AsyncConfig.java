package com.bamdoliro.maru.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    public static final String DOCUMENT_EXPORT_EXECUTOR = "documentExportExecutor";

    /**
     * 전체 자기소개서 PDF 생성 전용 스레드 풀.
     * PDF 생성은 오래 걸리고 메모리를 많이 쓰므로 동시에 2건까지만 돌리고, 나머지는 큐에서 기다리게 한다.
     * 요청 처리 스레드(톰캣)와 분리되어 있어 생성이 몰려도 헬스체크 등 다른 요청은 계속 처리된다.
     */
    @Bean(name = DOCUMENT_EXPORT_EXECUTOR)
    public ThreadPoolTaskExecutor documentExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("document-export-");
        executor.initialize();
        return executor;
    }
}
