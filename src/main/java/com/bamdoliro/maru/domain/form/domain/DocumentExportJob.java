package com.bamdoliro.maru.domain.form.domain;

import com.bamdoliro.maru.domain.form.domain.type.DocumentExportJobStatus;
import com.bamdoliro.maru.shared.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전체 자기소개서 PDF를 백그라운드에서 생성하는 작업.
 * PENDING -> IN_PROGRESS -> COMPLETED(s3Key 저장) 또는 FAILED(errorMessage 저장)로 진행된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_document_export_job")
@Entity
public class DocumentExportJob extends BaseTimeEntity {

    public static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    @Column(name = "document_export_job_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentExportJobStatus status;

    @Column
    private String s3Key;

    @Column(length = MAX_ERROR_MESSAGE_LENGTH)
    private String errorMessage;

    public static DocumentExportJob pending() {
        DocumentExportJob job = new DocumentExportJob();
        job.status = DocumentExportJobStatus.PENDING;
        return job;
    }

    public void start() {
        this.status = DocumentExportJobStatus.IN_PROGRESS;
    }

    public void complete(String s3Key) {
        this.status = DocumentExportJobStatus.COMPLETED;
        this.s3Key = s3Key;
        this.errorMessage = null;
    }

    public void fail(String errorMessage) {
        this.status = DocumentExportJobStatus.FAILED;
        this.errorMessage = truncate(errorMessage);
    }

    public boolean isCompleted() {
        return this.status == DocumentExportJobStatus.COMPLETED;
    }

    /**
     * S3에 저장할 파일 이름. 작업마다 겹치지 않도록 id를 쓴다.
     */
    public String getFileName() {
        return id + ".pdf";
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
