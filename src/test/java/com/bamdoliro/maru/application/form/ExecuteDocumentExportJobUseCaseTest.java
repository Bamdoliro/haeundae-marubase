package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import com.bamdoliro.maru.domain.form.domain.type.DocumentExportJobStatus;
import com.bamdoliro.maru.domain.form.exception.DocumentExportJobNotFoundException;
import com.bamdoliro.maru.infrastructure.pdf.exception.FailedToExportPdfException;
import com.bamdoliro.maru.infrastructure.persistence.form.DocumentExportJobRepository;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExecuteDocumentExportJobUseCaseTest {

    @InjectMocks
    private ExecuteDocumentExportJobUseCase executeDocumentExportJobUseCase;

    @Mock
    private DocumentExportJobRepository documentExportJobRepository;

    @Mock
    private ExportAllDocumentsUseCase exportAllDocumentsUseCase;

    @Mock
    private FileService fileService;

    private DocumentExportJob job;

    @BeforeEach
    void setUp() {
        job = DocumentExportJob.pending();
        ReflectionTestUtils.setField(job, "id", 3L);
    }

    private void givenJobIsSaved() {
        given(documentExportJobRepository.findById(3L)).willReturn(Optional.of(job));
        given(documentExportJobRepository.save(any(DocumentExportJob.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void PDF를_만들어_S3에_올리고_COMPLETED와_s3Key를_저장한다() {
        givenJobIsSaved();
        byte[] pdf = "<<pdf>>".getBytes();
        given(exportAllDocumentsUseCase.execute()).willReturn(new ByteArrayResource(pdf));
        given(fileService.upload(FolderConstant.DOCUMENT_EXPORT, "3.pdf", pdf, MediaType.APPLICATION_PDF_VALUE))
                .willReturn("document-export/3.pdf");

        executeDocumentExportJobUseCase.execute(3L);

        ArgumentCaptor<byte[]> uploaded = ArgumentCaptor.forClass(byte[].class);
        verify(fileService).upload(eq(FolderConstant.DOCUMENT_EXPORT), eq("3.pdf"), uploaded.capture(), eq(MediaType.APPLICATION_PDF_VALUE));
        assertArrayEquals(pdf, uploaded.getValue());
        assertEquals(DocumentExportJobStatus.COMPLETED, job.getStatus());
        assertEquals("document-export/3.pdf", job.getS3Key());
        assertNull(job.getErrorMessage());
        verify(documentExportJobRepository, times(2)).save(job);
    }

    @Test
    void 생성_중_예외가_나면_FAILED와_에러_메시지를_저장하고_업로드하지_않는다() {
        givenJobIsSaved();
        given(exportAllDocumentsUseCase.execute()).willThrow(new FailedToExportPdfException());

        executeDocumentExportJobUseCase.execute(3L);

        assertEquals(DocumentExportJobStatus.FAILED, job.getStatus());
        assertEquals(new FailedToExportPdfException().getMessage(), job.getErrorMessage());
        assertNull(job.getS3Key());
        verify(fileService, never()).upload(anyString(), anyString(), any(), anyString());
        verify(documentExportJobRepository, times(2)).save(job);
    }

    @Test
    void 메시지가_없는_예외면_예외_클래스_이름을_저장한다() {
        givenJobIsSaved();
        given(exportAllDocumentsUseCase.execute()).willThrow(new IllegalStateException());

        executeDocumentExportJobUseCase.execute(3L);

        assertEquals(DocumentExportJobStatus.FAILED, job.getStatus());
        assertEquals(IllegalStateException.class.getName(), job.getErrorMessage());
    }

    @Test
    void 에러_메시지가_너무_길면_최대_길이까지만_저장한다() {
        givenJobIsSaved();
        given(exportAllDocumentsUseCase.execute()).willThrow(new IllegalStateException("x".repeat(5000)));

        executeDocumentExportJobUseCase.execute(3L);

        assertEquals(DocumentExportJob.MAX_ERROR_MESSAGE_LENGTH, job.getErrorMessage().length());
    }

    @Test
    void S3_업로드에_실패해도_FAILED로_남긴다() {
        givenJobIsSaved();
        byte[] pdf = "<<pdf>>".getBytes();
        given(exportAllDocumentsUseCase.execute()).willReturn(new ByteArrayResource(pdf));
        given(fileService.upload(anyString(), anyString(), any(), anyString())).willThrow(new IllegalStateException("upload failed"));

        executeDocumentExportJobUseCase.execute(3L);

        assertEquals(DocumentExportJobStatus.FAILED, job.getStatus());
        assertEquals("upload failed", job.getErrorMessage());
    }

    @Test
    void 작업이_없으면_에러가_발생한다() {
        given(documentExportJobRepository.findById(3L)).willReturn(Optional.empty());

        assertThrows(DocumentExportJobNotFoundException.class, () -> executeDocumentExportJobUseCase.execute(3L));
        verify(exportAllDocumentsUseCase, never()).execute();
    }
}
