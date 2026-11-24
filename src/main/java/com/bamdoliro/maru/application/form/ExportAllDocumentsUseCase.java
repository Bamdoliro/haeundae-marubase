package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.pdf.GeneratePdfService;
import com.bamdoliro.maru.infrastructure.pdf.MergePdfService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.thymeleaf.ProcessTemplateService;
import com.bamdoliro.maru.infrastructure.thymeleaf.Templates;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@UseCase
public class ExportAllDocumentsUseCase {

    private final FormRepository formRepository;
    private final ProcessTemplateService processTemplateService;
    private final GeneratePdfService generatePdfService;
    private final MergePdfService mergePdfService;
    private final FileService fileService;
    private final AdmissionScheduleFacade admissionScheduleFacade;

    /**
     * 백그라운드 스레드(ExecuteDocumentExportJobUseCase)에는 요청 단위 영속성 컨텍스트가 없으므로,
     * 템플릿이 원서의 지연 로딩 연관 객체를 읽더라도 실패하지 않도록 읽기 전용 트랜잭션 안에서 실행한다.
     */
    @Transactional(readOnly = true)
    public ByteArrayResource execute() {
        List<Form> forms = formRepository.findDocumentExportTargets();
        AdmissionSchedule schedule = admissionScheduleFacade.getCurrentSchedule();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PdfDocument mergedDocument = new PdfDocument(new PdfWriter(outputStream))) {
            PdfMerger pdfMerger = new PdfMerger(mergedDocument);

            for(Form form : forms) {
                // 관리자가 언제 출력하든 서명란에는 지원자가 최종 제출한 날짜를 찍는다.
                // 제출 시각이 없는 기존 원서는 날짜가 비므로, null을 허용하지 않는 Map.of 대신 HashMap을 쓴다.
                Map<String, Object> documentMap = new HashMap<>();
                documentMap.put("form", form);
                documentMap.put("year", schedule.getAdmissionYear());
                documentMap.put("documentDate", form.getFinalSubmittedAt());

                String html1 = processTemplateService.execute(Templates.DOCUMENT_ONE, documentMap);
                ByteArrayOutputStream pdfStream1 = generatePdfService.execute(html1);
                mergePdfService.execute(pdfMerger, pdfStream1);

                String html2 = processTemplateService.execute(Templates.DOCUMENT_TWO, documentMap);
                ByteArrayOutputStream pdfStream2 = generatePdfService.execute(html2);
                mergePdfService.execute(pdfMerger, pdfStream2);
            }

            pdfMerger.close();
        }

        return new ByteArrayResource(outputStream.toByteArray());
    }
}
