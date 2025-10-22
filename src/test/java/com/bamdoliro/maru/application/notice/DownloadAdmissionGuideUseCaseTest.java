package com.bamdoliro.maru.application.notice;

import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.dto.response.UrlResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DownloadAdmissionGuideUseCaseTest {

    @InjectMocks
    private DownloadAdmissionGuideUseCase downloadAdmissionGuideUseCase;

    @Mock
    private FileService fileService;

    @Test
    void 입학전형요강을_다운로드한다() {
        // given
        String expectedUrl = "https://s3.amazonaws.com/haeundae-maru-s3-bucket/admission-guide/2026학년도 해운대고등학교 입학전형요강.pdf";
        doReturn(expectedUrl)
                .when(fileService)
                .getDownloadPresignedUrl(anyString(), anyString());

        // when
        UrlResponse response = downloadAdmissionGuideUseCase.execute();

        // then
        verify(fileService, times(1)).getDownloadPresignedUrl(anyString(), anyString());
        assertEquals(expectedUrl, response.getDownloadUrl());
        assertNull(response.getUploadUrl());
    }

    @Test
    void 입학전형요강_파일이_존재하지_않으면_null을_반환한다() {
        // given
        doReturn(null)
                .when(fileService)
                .getDownloadPresignedUrl(anyString(), anyString());

        // when
        UrlResponse response = downloadAdmissionGuideUseCase.execute();

        // then
        verify(fileService, times(1)).getDownloadPresignedUrl(anyString(), anyString());
        assertNull(response.getDownloadUrl());
        assertNull(response.getUploadUrl());
    }
}