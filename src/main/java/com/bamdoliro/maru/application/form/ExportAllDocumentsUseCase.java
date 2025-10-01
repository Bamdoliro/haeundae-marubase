package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
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

import java.io.ByteArrayOutputStream;
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

    public ByteArrayResource execute() {
        // 수정된 코드
        List<Form> forms = formRepository.findAll();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PdfDocument mergedDocument = new PdfDocument(new PdfWriter(outputStream))) {
            PdfMerger pdfMerger = new PdfMerger(mergedDocument);

            for(Form form : forms) {
                Map<String, Object> documentMap = Map.of(
                        "form", form
                );

                String html = processTemplateService.execute(Templates.DOCUMENT, documentMap);
                ByteArrayOutputStream pdfStream = generatePdfService.execute(html);
                mergePdfService.execute(pdfMerger, pdfStream);
            }

            pdfMerger.close();
        }

        return new ByteArrayResource(outputStream.toByteArray());
    }
}
