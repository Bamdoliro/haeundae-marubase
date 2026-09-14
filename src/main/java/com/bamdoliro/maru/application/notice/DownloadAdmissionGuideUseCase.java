package com.bamdoliro.maru.application.notice;

import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.infrastructure.s3.dto.response.UrlResponse;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;

@RequiredArgsConstructor
@UseCase
public class DownloadAdmissionGuideUseCase {

    private final FileService fileService;

    private static final String ADMISSION_GUIDE_FILENAME = "2027-admission-guide.pdf";
    private static final String ADMISSION_GUIDE_DISPLAY_FILENAME = "2027학년도 해운대고등학교 입학전형 요강.pdf";

    public UrlResponse execute() {
        String downloadUrl = fileService.getAttachmentDownloadPresignedUrl(
                FolderConstant.ADMISSION_GUIDE,
                ADMISSION_GUIDE_FILENAME,
                MediaType.APPLICATION_PDF_VALUE,
                ADMISSION_GUIDE_DISPLAY_FILENAME
        );
        return new UrlResponse(null, downloadUrl);
    }
}
