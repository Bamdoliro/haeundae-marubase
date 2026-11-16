package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import com.bamdoliro.maru.domain.form.domain.type.DocumentExportJobStatus;
import com.bamdoliro.maru.infrastructure.persistence.form.DocumentExportJobRepository;
import com.bamdoliro.maru.shared.response.IdResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StartDocumentExportJobUseCaseTest {

    @InjectMocks
    private StartDocumentExportJobUseCase startDocumentExportJobUseCase;

    @Mock
    private DocumentExportJobRepository documentExportJobRepository;

    @Mock
    private ExecuteDocumentExportJobUseCase executeDocumentExportJobUseCase;

    @Test
    void PENDING_작업을_저장하고_생성을_맡긴_뒤_작업_id를_돌려준다() {
        given(documentExportJobRepository.save(any(DocumentExportJob.class))).willAnswer(invocation -> {
            DocumentExportJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", 7L);
            return job;
        });

        IdResponse response = startDocumentExportJobUseCase.execute();

        assertEquals(7L, response.getId());
        ArgumentCaptor<DocumentExportJob> saved = ArgumentCaptor.forClass(DocumentExportJob.class);
        verify(documentExportJobRepository).save(saved.capture());
        assertEquals(DocumentExportJobStatus.PENDING, saved.getValue().getStatus());
        verify(executeDocumentExportJobUseCase).execute(7L);
    }

    @Test
    void 대기열이_가득_차_작업을_시작하지_못하면_FAILED로_남긴다() {
        given(documentExportJobRepository.save(any(DocumentExportJob.class))).willAnswer(invocation -> {
            DocumentExportJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", 7L);
            return job;
        });
        willThrow(new TaskRejectedException("full")).given(executeDocumentExportJobUseCase).execute(7L);

        IdResponse response = startDocumentExportJobUseCase.execute();

        assertEquals(7L, response.getId());
        ArgumentCaptor<DocumentExportJob> saved = ArgumentCaptor.forClass(DocumentExportJob.class);
        verify(documentExportJobRepository, times(2)).save(saved.capture());
        DocumentExportJob job = saved.getAllValues().get(1);
        assertEquals(DocumentExportJobStatus.FAILED, job.getStatus());
        assertEquals(StartDocumentExportJobUseCase.REJECTED_MESSAGE, job.getErrorMessage());
    }
}
