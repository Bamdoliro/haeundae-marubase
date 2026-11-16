package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import com.bamdoliro.maru.domain.form.domain.type.DocumentExportJobStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.infrastructure.message.SendMessageService;
import com.bamdoliro.maru.infrastructure.persistence.form.DocumentExportJobRepository;
import com.bamdoliro.maru.infrastructure.persistence.schedule.AdmissionScheduleRepository;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.presentation.form.dto.request.GenerateTestFormRequest;
import com.bamdoliro.maru.presentation.form.dto.request.TestFormCountRequest;
import com.bamdoliro.maru.presentation.form.dto.response.DocumentExportJobResponse;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;

/**
 * 실제 documentExportExecutor 스레드 풀과 S3(LocalStack)로 전체 자기소개서 PDF 생성 작업의 흐름을 확인한다.
 */
@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class DocumentExportJobSpringBootTest {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(60);

    @Autowired
    private StartDocumentExportJobUseCase startDocumentExportJobUseCase;

    @Autowired
    private QueryDocumentExportJobUseCase queryDocumentExportJobUseCase;

    @Autowired
    private DocumentExportJobRepository documentExportJobRepository;

    @Autowired
    private GenerateTestFormUseCase generateTestFormUseCase;

    @Autowired
    private AdmissionScheduleRepository admissionScheduleRepository;

    @Autowired
    private FileService fileService;

    @SpyBean
    private ExportAllDocumentsUseCase exportAllDocumentsUseCase;

    @MockBean
    private SendMessageService sendMessageService;

    @Test
    void 작업을_시작하면_PDF_생성이_끝나기를_기다리지_않고_바로_작업_id를_돌려준다() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        byte[] pdf = "<<pdf>>".getBytes();
        willAnswer(invocation -> {
            started.countDown();
            release.await(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            return new ByteArrayResource(pdf);
        }).given(exportAllDocumentsUseCase).execute();

        long begin = System.nanoTime();
        Long jobId = startDocumentExportJobUseCase.execute().getId();
        long elapsedMillis = (System.nanoTime() - begin) / 1_000_000;

        try {
            assertNotNull(jobId);
            assertTrue(elapsedMillis < 2_000, "작업 시작 응답이 " + elapsedMillis + "ms 걸렸습니다.");
            assertTrue(started.await(10, TimeUnit.SECONDS), "백그라운드에서 PDF 생성이 시작되지 않았습니다.");

            DocumentExportJobResponse inProgress = queryDocumentExportJobUseCase.execute(jobId);
            assertEquals(DocumentExportJobStatus.IN_PROGRESS, inProgress.getStatus());
            assertNull(inProgress.getDownloadUrl());
        } finally {
            release.countDown();
        }

        DocumentExportJobResponse completed = awaitFinished(jobId);
        assertEquals(DocumentExportJobStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getDownloadUrl());
        assertArrayEquals(pdf, fileService.download(FolderConstant.DOCUMENT_EXPORT, jobId + ".pdf"));
    }

    @Test
    void 완료된_작업을_조회하면_COMPLETED와_실제_PDF를_내려받는_presigned_URL이_온다() throws Exception {
        admissionScheduleRepository.save(ScheduleFixture.createSchedule(2027, true));
        generateTestFormUseCase.execute(new GenerateTestFormRequest(List.of(
                new TestFormCountRequest(FormType.REGULAR, 2),
                new TestFormCountRequest(FormType.MULTICULTURAL, 1)
        )));

        Long jobId = startDocumentExportJobUseCase.execute().getId();
        DocumentExportJobResponse response = awaitFinished(jobId);

        assertEquals(DocumentExportJobStatus.COMPLETED, response.getStatus(), response.getErrorMessage());
        assertNull(response.getErrorMessage());
        DocumentExportJob job = documentExportJobRepository.findById(jobId).orElseThrow();
        assertEquals(FolderConstant.DOCUMENT_EXPORT + "/" + jobId + ".pdf", job.getS3Key());

        HttpResponse<byte[]> download = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(response.getDownloadUrl())).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );
        assertEquals(200, download.statusCode());
        String encodedFileName = URLEncoder.encode("전체 자기소개서.pdf", StandardCharsets.UTF_8).replace("+", "%20");
        assertTrue(download.headers().firstValue("Content-Disposition").orElse("").contains(encodedFileName));
        // 원서 1건당 자기소개서 2쪽
        assertEquals(6, pages(download.body()));
    }

    @Test
    void PDF_생성_중_예외가_나면_FAILED와_에러_메시지가_저장된다() throws Exception {
        willThrow(new IllegalStateException("PDF 생성 실패 테스트")).given(exportAllDocumentsUseCase).execute();

        Long jobId = startDocumentExportJobUseCase.execute().getId();
        DocumentExportJobResponse response = awaitFinished(jobId);

        assertEquals(DocumentExportJobStatus.FAILED, response.getStatus());
        assertEquals("PDF 생성 실패 테스트", response.getErrorMessage());
        assertNull(response.getDownloadUrl());
        DocumentExportJob job = documentExportJobRepository.findById(jobId).orElseThrow();
        assertEquals("PDF 생성 실패 테스트", job.getErrorMessage());
        assertNull(job.getS3Key());
    }

    private DocumentExportJobResponse awaitFinished(Long jobId) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
        DocumentExportJobResponse response = queryDocumentExportJobUseCase.execute(jobId);
        while (!isFinished(response.getStatus()) && System.nanoTime() < deadline) {
            Thread.sleep(200);
            response = queryDocumentExportJobUseCase.execute(jobId);
        }
        assertNotEquals(DocumentExportJobStatus.PENDING, response.getStatus(), "작업이 시작되지 않았습니다.");
        assertTrue(isFinished(response.getStatus()), "작업이 제한 시간 안에 끝나지 않았습니다: " + response.getStatus());
        return response;
    }

    private static boolean isFinished(DocumentExportJobStatus status) {
        return status == DocumentExportJobStatus.COMPLETED || status == DocumentExportJobStatus.FAILED;
    }

    private static int pages(byte[] pdf) {
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
