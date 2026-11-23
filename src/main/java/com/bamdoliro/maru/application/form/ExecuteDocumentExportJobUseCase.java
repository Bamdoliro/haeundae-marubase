package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import com.bamdoliro.maru.domain.form.exception.DocumentExportJobNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.form.DocumentExportJobRepository;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;

/**
 * documentExportExecutor 스레드에서 전체 자기소개서 PDF를 만들어 S3에 올리고, 작업 상태를 갱신한다.
 * 상태 변경은 각각 바로 커밋되므로, 생성 중에도 조회 API에서 IN_PROGRESS를 볼 수 있다.
 * StartDocumentExportJobUseCase가 얻은 DocumentExportLock은 성공/실패와 관계없이 여기서 놓는다.
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class ExecuteDocumentExportJobUseCase {

    private final DocumentExportJobRepository documentExportJobRepository;
    private final ExportAllDocumentsUseCase exportAllDocumentsUseCase;
    private final FileService fileService;
    private final DocumentExportLock documentExportLock;

    @Async(AsyncConfig.DOCUMENT_EXPORT_EXECUTOR)
    public void execute(Long jobId) {
        boolean released = false;
        try {
            DocumentExportJob job = documentExportJobRepository.findById(jobId)
                    .orElseThrow(DocumentExportJobNotFoundException::new);
            job.start();
            job = documentExportJobRepository.save(job);

            try {
                byte[] pdf = exportAllDocumentsUseCase.execute().getByteArray();
                String s3Key = fileService.upload(FolderConstant.DOCUMENT_EXPORT, job.getFileName(), pdf, MediaType.APPLICATION_PDF_VALUE);
                job.complete(s3Key);
                log.info("Document export job {} completed: {} ({} bytes)", jobId, s3Key, pdf.length);
            } catch (Exception | OutOfMemoryError e) {
                log.error("Document export job {} failed", jobId, e);
                job.fail(messageOf(e));
            }

            // 조회 API에서 끝난 상태를 본 뒤 바로 새 작업을 시작해도 이전 작업 id를 돌려받지 않도록, 상태를 저장하기 전에 놓는다.
            documentExportLock.release();
            released = true;
            documentExportJobRepository.save(job);
        } finally {
            if (!released) {
                documentExportLock.release();
            }
        }
    }

    private static String messageOf(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getName();
    }
}
