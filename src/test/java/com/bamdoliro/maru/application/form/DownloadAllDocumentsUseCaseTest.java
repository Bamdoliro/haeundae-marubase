package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.exception.DocumentExportInProgressException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DownloadAllDocumentsUseCaseTest {

    @Mock
    private ExportAllDocumentsUseCase exportAllDocumentsUseCase;

    private DocumentExportLock lock;
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        lock = new DocumentExportLock();
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("test-document-export-");
        executor.initialize();
    }

    @Test
    void 전용_스레드에서_PDF를_만들고_끝나면_잠금을_놓는다() throws Exception {
        byte[] pdf = "<<pdf>>".getBytes();
        AtomicReference<String> threadName = new AtomicReference<>();
        willAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            return new ByteArrayResource(pdf);
        }).given(exportAllDocumentsUseCase).execute();

        ByteArrayResource result = new DownloadAllDocumentsUseCase(exportAllDocumentsUseCase, lock, executor)
                .execute().get(10, TimeUnit.SECONDS);

        assertArrayEquals(pdf, result.getByteArray());
        assertTrue(threadName.get().startsWith("test-document-export-"), threadName.get());
        assertReleased();
    }

    @Test
    void 생성이_진행_중이면_새로_만들지_않고_거부한다() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        willAnswer(invocation -> {
            release.await(10, TimeUnit.SECONDS);
            return new ByteArrayResource(new byte[0]);
        }).given(exportAllDocumentsUseCase).execute();
        DownloadAllDocumentsUseCase useCase = new DownloadAllDocumentsUseCase(exportAllDocumentsUseCase, lock, executor);

        CompletableFuture<ByteArrayResource> first = useCase.execute();
        try {
            assertThrows(DocumentExportInProgressException.class, useCase::execute);
        } finally {
            release.countDown();
        }

        first.get(10, TimeUnit.SECONDS);
        verify(exportAllDocumentsUseCase).execute();
        assertReleased();
    }

    @Test
    void 생성이_실패해도_잠금을_놓는다() {
        willThrow(new IllegalStateException("boom")).given(exportAllDocumentsUseCase).execute();

        CompletableFuture<ByteArrayResource> future =
                new DownloadAllDocumentsUseCase(exportAllDocumentsUseCase, lock, executor).execute();

        ExecutionException e = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, e.getCause());
        assertReleased();
    }

    @Test
    void 실행기가_작업을_거부하면_잠금을_놓고_거부한다() {
        TaskExecutor rejecting = mock(TaskExecutor.class);
        willThrow(new TaskRejectedException("full")).given(rejecting).execute(any());

        assertThrows(DocumentExportInProgressException.class,
                () -> new DownloadAllDocumentsUseCase(exportAllDocumentsUseCase, lock, rejecting).execute());

        verify(exportAllDocumentsUseCase, never()).execute();
        assertReleased();
    }

    private void assertReleased() {
        // whenComplete 콜백은 future 완료 직후 같은 스레드에서 돌므로 잠깐 기다린다.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!lock.tryAcquire()) {
            assertFalse(System.nanoTime() > deadline, "잠금이 풀리지 않았습니다.");
            Thread.onSpinWait();
        }
        lock.release();
        assertEquals(null, lock.getRunningJobId());
    }
}
