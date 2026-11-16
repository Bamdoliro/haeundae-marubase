package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import com.bamdoliro.maru.domain.form.domain.type.DocumentExportJobStatus;
import com.bamdoliro.maru.domain.form.exception.DocumentExportJobNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.form.DocumentExportJobRepository;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.presentation.form.dto.response.DocumentExportJobResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueryDocumentExportJobUseCaseTest {

    @InjectMocks
    private QueryDocumentExportJobUseCase queryDocumentExportJobUseCase;

    @Mock
    private DocumentExportJobRepository documentExportJobRepository;

    @Mock
    private FileService fileService;

    private static DocumentExportJob jobWithId(long id) {
        DocumentExportJob job = DocumentExportJob.pending();
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    @Test
    void 완료된_작업이면_전체_자기소개서_파일명의_다운로드_URL을_함께_돌려준다() {
        DocumentExportJob job = jobWithId(3L);
        job.complete("document-export/3.pdf");
        given(documentExportJobRepository.findById(3L)).willReturn(Optional.of(job));
        given(fileService.getAttachmentDownloadPresignedUrl(
                FolderConstant.DOCUMENT_EXPORT, "3.pdf", MediaType.APPLICATION_PDF_VALUE, "전체 자기소개서.pdf"
        )).willReturn("https://s3/document-export/3.pdf?signature");

        DocumentExportJobResponse response = queryDocumentExportJobUseCase.execute(3L);

        assertEquals(3L, response.getId());
        assertEquals(DocumentExportJobStatus.COMPLETED, response.getStatus());
        assertEquals("https://s3/document-export/3.pdf?signature", response.getDownloadUrl());
        assertNull(response.getErrorMessage());
    }

    @Test
    void 진행_중인_작업이면_다운로드_URL_없이_상태만_돌려준다() {
        DocumentExportJob job = jobWithId(3L);
        job.start();
        given(documentExportJobRepository.findById(3L)).willReturn(Optional.of(job));

        DocumentExportJobResponse response = queryDocumentExportJobUseCase.execute(3L);

        assertEquals(DocumentExportJobStatus.IN_PROGRESS, response.getStatus());
        assertNull(response.getDownloadUrl());
        verify(fileService, never()).getAttachmentDownloadPresignedUrl(anyString(), anyString(), any(), any());
    }

    @Test
    void 실패한_작업이면_에러_메시지를_돌려준다() {
        DocumentExportJob job = jobWithId(3L);
        job.fail("PDF 변환에 실패했습니다.");
        given(documentExportJobRepository.findById(3L)).willReturn(Optional.of(job));

        DocumentExportJobResponse response = queryDocumentExportJobUseCase.execute(3L);

        assertEquals(DocumentExportJobStatus.FAILED, response.getStatus());
        assertEquals("PDF 변환에 실패했습니다.", response.getErrorMessage());
        assertNull(response.getDownloadUrl());
    }

    @Test
    void 작업이_없으면_에러가_발생한다() {
        given(documentExportJobRepository.findById(3L)).willReturn(Optional.empty());

        assertThrows(DocumentExportJobNotFoundException.class, () -> queryDocumentExportJobUseCase.execute(3L));
    }
}
