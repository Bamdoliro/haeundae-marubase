package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.infrastructure.pdf.GeneratePdfService;
import com.bamdoliro.maru.infrastructure.pdf.MergePdfService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.infrastructure.thymeleaf.ProcessTemplateService;
import com.bamdoliro.maru.infrastructure.thymeleaf.Templates;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.constants.Schedule;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@UseCase
public class GenerateAllAdmissionTicketUseCase {

    private final FormRepository formRepository;
    private final ProcessTemplateService processTemplateService;
    private final GeneratePdfService generatePdfService;
    private final MergePdfService mergePdfService;
    private final FileService fileService;

    public ByteArrayResource execute() {
        List<Form> formList = formRepository.findByStatus(FormStatus.FIRST_PASSED).stream()
                .sorted(Comparator.comparing(Form::getExaminationNumber, Comparator.nullsLast(Long::compareTo)))
                .toList();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfDocument mergedDocument = new PdfDocument(new PdfWriter(outputStream));
        PdfMerger pdfMerger = new PdfMerger(mergedDocument);

        formList.forEach(form -> mergePdfService.execute(pdfMerger, generateAdmissionTicket(form)));

        mergedDocument.close();
        pdfMerger.close();

        return new ByteArrayResource(outputStream.toByteArray());
    }

    private ByteArrayOutputStream generateAdmissionTicket(Form form) {

        // 신분증 사진이 S3에 없으면 presigned URL이 null이라, null을 허용하지 않는 Map.of/Map.ofEntries 대신 HashMap을 쓴다
        Map<String, Object> formMap = new HashMap<>();
        formMap.put("form", form);
        formMap.put("year", Schedule.getAdmissionYear());
        formMap.put("depthInterview", Schedule.toLocaleString(Schedule.DEPTH_INTERVIEW));
        formMap.put("announcementOfSecondPass", Schedule.toLocaleString(Schedule.ANNOUNCEMENT_OF_SECOND_PASS));
        formMap.put("entranceRegistrationTime", Schedule.toLocaleString(Schedule.ENTRANCE_REGISTRATION_PERIOD_START, Schedule.ENTRANCE_REGISTRATION_PERIOD_END));
        formMap.put("identificationPictureUri", fileService.getDownloadPresignedUrl(FolderConstant.IDENTIFICATION_PICTURE, form.getUser().getUuid().toString()));
        String html = processTemplateService.execute(Templates.ADMISSION_TICKET, formMap);

        return generatePdfService.execute(html);
    }
}
