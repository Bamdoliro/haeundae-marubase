package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.exception.DocumentExportInProgressException;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.config.AsyncConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

/**
 * 전체 자기소개서 PDF를 바로 내려받는 요청을 처리한다.
 * 생성은 documentExportExecutor 스레드에서 하고 톰캣 스레드는 즉시 반납하므로(MVC 비동기 응답),
 * 생성이 CPU를 오래 써도 헬스체크와 다른 API 요청을 처리할 스레드가 남는다.
 * 이미 다른 생성이 진행 중이면 중복으로 만들지 않고 바로 거부한다.
 */
@UseCase
public class DownloadAllDocumentsUseCase {

    private final ExportAllDocumentsUseCase exportAllDocumentsUseCase;
    private final DocumentExportLock documentExportLock;
    private final TaskExecutor documentExportExecutor;

    public DownloadAllDocumentsUseCase(
            ExportAllDocumentsUseCase exportAllDocumentsUseCase,
            DocumentExportLock documentExportLock,
            @Qualifier(AsyncConfig.DOCUMENT_EXPORT_EXECUTOR) TaskExecutor documentExportExecutor
    ) {
        this.exportAllDocumentsUseCase = exportAllDocumentsUseCase;
        this.documentExportLock = documentExportLock;
        this.documentExportExecutor = documentExportExecutor;
    }

    public CompletableFuture<ByteArrayResource> execute() {
        if (!documentExportLock.tryAcquire()) {
            throw new DocumentExportInProgressException();
        }

        try {
            return CompletableFuture
                    .supplyAsync(exportAllDocumentsUseCase::execute, documentExportExecutor)
                    .whenComplete((result, error) -> documentExportLock.release());
        } catch (RejectedExecutionException e) {
            documentExportLock.release();
            throw new DocumentExportInProgressException();
        }
    }
}
