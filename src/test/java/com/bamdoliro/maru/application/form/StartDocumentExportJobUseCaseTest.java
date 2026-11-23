package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import com.bamdoliro.maru.domain.form.domain.type.DocumentExportJobStatus;
import com.bamdoliro.maru.domain.form.exception.DocumentExportInProgressException;
import com.bamdoliro.maru.infrastructure.persistence.form.DocumentExportJobRepository;
import com.bamdoliro.maru.shared.response.IdResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
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

    @Spy
    private DocumentExportLock documentExportLock = new DocumentExportLock();

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
        // 잠금은 생성이 끝날 때 ExecuteDocumentExportJobUseCase가 놓는다.
        assertFalse(documentExportLock.tryAcquire());
        assertEquals(7L, documentExportLock.getRunningJobId());
    }

    @Test
    void 이미_진행_중인_작업이_있으면_새로_만들지_않고_그_작업_id를_돌려준다() {
        documentExportLock.tryAcquire();
        documentExportLock.holdFor(3L);

        IdResponse response = startDocumentExportJobUseCase.execute();

        assertEquals(3L, response.getId());
        verify(documentExportJobRepository, never()).save(any());
        verify(executeDocumentExportJobUseCase, never()).execute(any());
    }

    @Test
    void 바로_내려받기로_생성_중이면_거부한다() {
        documentExportLock.tryAcquire();

        assertThrows(DocumentExportInProgressException.class, () -> startDocumentExportJobUseCase.execute());

        verify(documentExportJobRepository, never()).save(any());
        verify(executeDocumentExportJobUseCase, never()).execute(any());
    }

    @Test
    void 작업_저장에_실패하면_잠금을_놓는다() {
        given(documentExportJobRepository.save(any(DocumentExportJob.class))).willThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> startDocumentExportJobUseCase.execute());

        assertTrue(documentExportLock.tryAcquire());
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
        assertNull(documentExportLock.getRunningJobId());
        assertTrue(documentExportLock.tryAcquire());
    }
}
