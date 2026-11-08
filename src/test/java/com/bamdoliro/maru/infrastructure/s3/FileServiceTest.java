package com.bamdoliro.maru.infrastructure.s3;

import com.bamdoliro.maru.infrastructure.s3.dto.request.FileMetadata;
import com.bamdoliro.maru.infrastructure.s3.dto.response.UrlResponse;
import com.bamdoliro.maru.infrastructure.s3.validator.DefaultFileValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import com.bamdoliro.maru.infrastructure.s3.exception.S3FileNotFoundException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static com.bamdoliro.maru.shared.constants.FileConstant.MB;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Test
    void Presigned_URL을_생성한다() throws Exception {
        // given
        String url = "https://bucket.s3.ap-northeast-2.amazonaws.com/random-uuid-image.png";
        FileMetadata fileMetadata = new FileMetadata(
                "image.png",
                MediaType.IMAGE_PNG_VALUE,
                MB
        );

        PresignedPutObjectRequest putPresignedRequest = mock(PresignedPutObjectRequest.class);
        given(putPresignedRequest.url()).willReturn(new URL(url));

        PresignedGetObjectRequest getPresignedRequest = mock(PresignedGetObjectRequest.class);
        given(getPresignedRequest.url()).willReturn(new URL(url));

        given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(HeadObjectResponse.builder().build());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(putPresignedRequest);
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(getPresignedRequest);

        // when
        UrlResponse response = fileService.getPresignedUrl("folder", "uuid", fileMetadata, metadata ->
                DefaultFileValidator.validate(metadata, Set.of(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG), 2)
        );

        // then
        verify(s3Presigner, times(1)).presignPutObject(any(PutObjectPresignRequest.class));
        verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void S3_객체를_바이트로_내려받는다() {
        byte[] bytes = "pdf-bytes".getBytes();
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes));

        assertArrayEquals(bytes, fileService.download("form", "uuid"));
    }

    @Test
    void 내려받을_S3_객체가_없으면_에러가_발생한다() {
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willThrow(NoSuchKeyException.builder().build());

        assertThrows(S3FileNotFoundException.class, () -> fileService.download("form", "uuid"));
    }
}
