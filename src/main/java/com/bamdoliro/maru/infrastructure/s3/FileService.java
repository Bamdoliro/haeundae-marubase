package com.bamdoliro.maru.infrastructure.s3;

import com.bamdoliro.maru.infrastructure.s3.dto.response.UrlResponse;
import com.bamdoliro.maru.infrastructure.s3.validator.FileValidator;
import com.bamdoliro.maru.infrastructure.s3.dto.request.FileMetadata;
import com.bamdoliro.maru.infrastructure.s3.exception.S3AccessFailedException;
import com.bamdoliro.maru.infrastructure.s3.exception.S3FileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String getUploadPresignedUrl(String folder, String fileName, FileMetadata fileMetadata, FileValidator validator) {
        validator.validate(fileMetadata);
        String fullFileName = createFileName(folder, fileName);
        PutObjectPresignRequest request = getGenerateUploadPresignedUrlRequest(bucket, fullFileName, fileMetadata);

        return s3Presigner.presignPutObject(request).url().toString();
    }

    public String getDownloadPresignedUrl(String folder, String fileName) {
        return getDownloadPresignedUrl(folder, fileName, null, null);
    }

    public String getAttachmentDownloadPresignedUrl(String folder, String fileName, String contentType, String displayFileName) {
        return getDownloadPresignedUrl(folder, fileName, contentType, createAttachmentContentDisposition(displayFileName));
    }

    private String getDownloadPresignedUrl(String folder, String fileName, String contentType, String contentDisposition) {
        String fullFileName = createFileName(folder, fileName);
        GetObjectPresignRequest request = getGenerateDownloadPresignedUrlRequest(bucket, fullFileName, contentType, contentDisposition);

        return request != null ? s3Presigner.presignGetObject(request).url().toString() : null;
    }

    /**
     * 서버에서 S3 객체를 바이트로 직접 받아온다.
     * 객체가 없으면 404(S3FileNotFoundException), 권한 부족(403) 등 그 외 S3 오류는 S3AccessFailedException.
     */
    public byte[] download(String folder, String fileName) {
        String fullFileName = createFileName(folder, fileName);
        try {
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(fullFileName)
                            .build()
            ).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new S3FileNotFoundException();
        } catch (S3Exception e) {
            throw accessFailed("download", fullFileName, e);
        }
    }

    public UrlResponse getPresignedUrl(String folder, String fileName, FileMetadata metadata, FileValidator validator) {
        return new UrlResponse(
                getUploadPresignedUrl(folder, fileName, metadata, validator),
                getDownloadPresignedUrl(folder, fileName)
        );
    }

    private PutObjectPresignRequest getGenerateUploadPresignedUrlRequest(String bucket, String fileName, FileMetadata fileMetadata) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(fileMetadata.getMediaType())
                .contentLength(fileMetadata.getFileSize())
                .build();

        return PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(3))
                .putObjectRequest(objectRequest)
                .build();
    }

    private GetObjectPresignRequest getGenerateDownloadPresignedUrlRequest(String bucket, String fileName, String contentType, String contentDisposition) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build());
        } catch (NoSuchKeyException e) {
            return null;
        } catch (S3Exception e) {
            throw accessFailed("headObject", fileName, e);
        }

        return GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60 * 10))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileName)
                        .responseContentType(contentType)
                        .responseContentDisposition(contentDisposition)
                        .build())
                .build();
    }

    private S3AccessFailedException accessFailed(String operation, String key, S3Exception e) {
        log.error(
                "S3 {} 실패 [bucket={}, key={}, status={}, errorCode={}, requestId={}]",
                operation,
                bucket,
                key,
                e.statusCode(),
                e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null,
                e.requestId(),
                e
        );
        return new S3AccessFailedException();
    }

    private String createAttachmentContentDisposition(String displayFileName) {
        if (displayFileName == null) {
            return null;
        }

        String encodedFileName = URLEncoder.encode(displayFileName, StandardCharsets.UTF_8).replace("+", "%20");

        return String.format("attachment; filename*=UTF-8''%s", encodedFileName);
    }

    private String createFileName(String folder, String fileName) {
        return folder + "/" + fileName;
    }
}
