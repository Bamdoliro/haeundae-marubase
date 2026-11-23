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
     * PDF 변환은 CPU와 메모리를 오래 점유하므로 스레드를 1개로 두어 코어 하나 이상을 쓰지 않게 한다.
     * 요청 처리 스레드(톰캣)와 분리되어 있어 생성 중에도 헬스체크 등 다른 요청은 계속 처리된다.
     * 동시에 하나만 접수되도록 DocumentExportLock이 막으므로 큐는 작게 둔다.
     */
    @Bean(name = DOCUMENT_EXPORT_EXECUTOR)
    public ThreadPoolTaskExecutor documentExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("document-export-");
        executor.initialize();
        return executor;
    }
}
