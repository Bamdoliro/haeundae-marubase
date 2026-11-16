package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import com.bamdoliro.maru.infrastructure.persistence.form.DocumentExportJobRepository;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.response.IdResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;

/**
 * 전체 자기소개서 PDF 생성 작업을 PENDING으로 만들고, 생성은 백그라운드에 맡긴 채 작업 id를 바로 돌려준다.
 * 비동기 스레드가 작업을 조회할 수 있도록 저장이 먼저 커밋되어야 하므로 이 메서드에는 트랜잭션을 걸지 않는다.
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class StartDocumentExportJobUseCase {

    static final String REJECTED_MESSAGE = "대기 중인 PDF 생성 작업이 많아 시작하지 못했습니다. 잠시 후 다시 시도해주세요.";

    private final DocumentExportJobRepository documentExportJobRepository;
    private final ExecuteDocumentExportJobUseCase executeDocumentExportJobUseCase;

    public IdResponse execute() {
        DocumentExportJob job = documentExportJobRepository.save(DocumentExportJob.pending());

        try {
            executeDocumentExportJobUseCase.execute(job.getId());
        } catch (TaskRejectedException e) {
            log.warn("Document export job {} was rejected by the executor", job.getId());
            job.fail(REJECTED_MESSAGE);
            documentExportJobRepository.save(job);
        }

        return new IdResponse(job.getId());
    }
}
