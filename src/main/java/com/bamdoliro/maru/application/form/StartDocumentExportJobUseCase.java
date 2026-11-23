package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import com.bamdoliro.maru.domain.form.exception.DocumentExportInProgressException;
import com.bamdoliro.maru.infrastructure.persistence.form.DocumentExportJobRepository;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.response.IdResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;

/**
 * 전체 자기소개서 PDF 생성 작업을 PENDING으로 만들고, 생성은 백그라운드에 맡긴 채 작업 id를 바로 돌려준다.
 * 비동기 스레드가 작업을 조회할 수 있도록 저장이 먼저 커밋되어야 하므로 이 메서드에는 트랜잭션을 걸지 않는다.
 * 이미 작업이 진행 중이면 새로 만들지 않고 그 작업 id를 돌려주고,
 * 바로 내려받기(DownloadAllDocumentsUseCase)로 생성 중이면 거부한다.
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class StartDocumentExportJobUseCase {

    static final String REJECTED_MESSAGE = "대기 중인 PDF 생성 작업이 많아 시작하지 못했습니다. 잠시 후 다시 시도해주세요.";

    private final DocumentExportJobRepository documentExportJobRepository;
    private final ExecuteDocumentExportJobUseCase executeDocumentExportJobUseCase;
    private final DocumentExportLock documentExportLock;

    /**
     * 동시에 호출돼도 작업이 둘 만들어지지 않도록, 잠금 획득과 작업 id 기록을 한 번에 처리한다.
     */
    public synchronized IdResponse execute() {
        if (!documentExportLock.tryAcquire()) {
            Long runningJobId = documentExportLock.getRunningJobId();
            if (runningJobId == null) {
                throw new DocumentExportInProgressException();
            }
            log.info("Document export job {} is already running; returning it", runningJobId);
            return new IdResponse(runningJobId);
        }

        DocumentExportJob job;
        try {
            job = documentExportJobRepository.save(DocumentExportJob.pending());
            documentExportLock.holdFor(job.getId());
        } catch (RuntimeException e) {
            documentExportLock.release();
            throw e;
        }

        try {
            executeDocumentExportJobUseCase.execute(job.getId());
        } catch (TaskRejectedException e) {
            log.warn("Document export job {} was rejected by the executor", job.getId());
            documentExportLock.release();
            job.fail(REJECTED_MESSAGE);
            documentExportJobRepository.save(job);
        }

        return new IdResponse(job.getId());
    }
}
