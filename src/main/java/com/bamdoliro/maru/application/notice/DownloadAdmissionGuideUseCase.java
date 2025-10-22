package com.bamdoliro.maru.application.notice;

import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.infrastructure.s3.dto.response.UrlResponse;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class DownloadAdmissionGuideUseCase {

    private final FileService fileService;

    private static final String ADMISSION_GUIDE_FILENAME = "2026-admission-guide.pdf";

    public UrlResponse execute() {
        String downloadUrl = fileService.getDownloadPresignedUrl(FolderConstant.ADMISSION_GUIDE, ADMISSION_GUIDE_FILENAME);
        return new UrlResponse(null, downloadUrl);
    }
}
