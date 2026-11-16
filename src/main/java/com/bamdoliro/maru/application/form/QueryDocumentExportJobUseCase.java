package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import com.bamdoliro.maru.domain.form.exception.DocumentExportJobNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.form.DocumentExportJobRepository;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.presentation.form.dto.response.DocumentExportJobResponse;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;

/**
 * 전체 자기소개서 PDF 생성 작업의 상태를 조회한다. 완료된 작업이면 다운로드용 presigned URL을 함께 돌려준다.
 */
@RequiredArgsConstructor
@UseCase
public class QueryDocumentExportJobUseCase {

    static final String DOWNLOAD_FILE_NAME = "전체 자기소개서.pdf";

    private final DocumentExportJobRepository documentExportJobRepository;
    private final FileService fileService;

    public DocumentExportJobResponse execute(Long jobId) {
        DocumentExportJob job = documentExportJobRepository.findById(jobId)
                .orElseThrow(DocumentExportJobNotFoundException::new);

        String downloadUrl = job.isCompleted()
                ? fileService.getAttachmentDownloadPresignedUrl(
                        FolderConstant.DOCUMENT_EXPORT,
                        job.getFileName(),
                        MediaType.APPLICATION_PDF_VALUE,
                        DOWNLOAD_FILE_NAME
                )
                : null;

        return new DocumentExportJobResponse(job, downloadUrl);
    }
}
