package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.exception.InvalidFormStatusException;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.pdf.GeneratePdfService;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.infrastructure.thymeleaf.ProcessTemplateService;
import com.bamdoliro.maru.infrastructure.thymeleaf.Templates;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.constants.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@UseCase
public class GenerateAdmissionTicketUseCase {

    private final FormFacade formFacade;
    private final ProcessTemplateService processTemplateService;
    private final GeneratePdfService generatePdfService;
    private final FileService fileService;
    private final AdmissionScheduleFacade admissionScheduleFacade;

    public ByteArrayResource execute(User user) {
        Form form = formFacade.getForm(user);
        validateFormStatus(form);
        AdmissionSchedule schedule = admissionScheduleFacade.getCurrentSchedule();

        // 신분증 사진이 S3에 없으면 presigned URL이 null이라, null을 허용하지 않는 Map.of/Map.ofEntries 대신 HashMap을 쓴다
        Map<String, Object> formMap = new HashMap<>();
        formMap.put("form", form);
        formMap.put("year", schedule.getAdmissionYear());
        formMap.put("depthInterview", Schedule.toLocaleString(schedule.getInterviewStart()));
        formMap.put("announcementOfSecondPass", Schedule.toLocaleString(schedule.getAnnouncementOfFinalPass()));
        formMap.put("entranceRegistrationTime", Schedule.toLocaleString(schedule.getRegistrationStart(), schedule.getRegistrationEnd()));
        formMap.put("identificationPictureUri", fileService.getDownloadPresignedUrl(FolderConstant.IDENTIFICATION_PICTURE, user.getUuid().toString()));
        String html = processTemplateService.execute(Templates.ADMISSION_TICKET, formMap);
        ByteArrayOutputStream outputStream = generatePdfService.execute(html);

        return new ByteArrayResource(outputStream.toByteArray());
    }

    private void validateFormStatus(Form form) {
        if (!form.isFirstPassedNow()) {
            throw new InvalidFormStatusException();
        }
    }
}
