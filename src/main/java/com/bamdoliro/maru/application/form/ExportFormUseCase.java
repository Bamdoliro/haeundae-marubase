package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.AchievementLevel;
import com.bamdoliro.maru.domain.form.domain.value.Subject;
import com.bamdoliro.maru.domain.form.domain.value.SubjectMap;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.pdf.GeneratePdfService;
import com.bamdoliro.maru.infrastructure.pdf.MergePdfService;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.infrastructure.thymeleaf.ProcessTemplateService;
import com.bamdoliro.maru.infrastructure.thymeleaf.Templates;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.constants.Schedule;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class ExportFormUseCase {

    private final FormFacade formFacade;
    private final ProcessTemplateService processTemplateService;
    private final GeneratePdfService generatePdfService;
    private final MergePdfService mergePdfService;
    private final FileService fileService;
    private final AdmissionScheduleFacade admissionScheduleFacade;

    public ByteArrayResource execute(User user) {
        Form form = formFacade.getForm(user);
        AdmissionSchedule schedule = admissionScheduleFacade.getCurrentSchedule();

        SubjectMap subjectMap = form.getGrade().getSubjectList().getSubjectMap();
        // 신분증 사진이 S3에 없으면 presigned URL이 null이라, null을 허용하지 않는 Map.of 대신 HashMap을 쓴다
        Map<String, Object> formMap = new HashMap<>();
        formMap.put("form", form);
        formMap.put("grade21", subjectMap.getSubjectListOf(2, 1));
        formMap.put("grade22", subjectMap.getSubjectListOf(2, 2));
        formMap.put("grade31", subjectMap.getSubjectListOf(3, 1));
        formMap.put("grade32", subjectMap.getSubjectListOf(3, 2));
        formMap.put("subjectList", getSubjectList(form));
        formMap.put("year", schedule.getAdmissionYear());
        formMap.put("applicationDeadline", Schedule.toShortLocaleString(schedule.getApplicationFormEnd()));
        formMap.put("identificationPictureUri", fileService.getDownloadPresignedUrl(FolderConstant.IDENTIFICATION_PICTURE, user.getUuid().toString()));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfDocument mergedDocument = new PdfDocument(new PdfWriter(outputStream));
        PdfMerger pdfMerger = new PdfMerger(mergedDocument);

        getRequiredTemplates(form)
                .stream()
                .map((t) -> processTemplateService.execute(t, formMap))
                .map(generatePdfService::execute)
                .forEach((s) -> mergePdfService.execute(pdfMerger, s));

        mergedDocument.close();
        pdfMerger.close();

        return new ByteArrayResource(outputStream.toByteArray());
    }

    private List<SubjectVO> getSubjectList(Form form) {
        List<SubjectVO> value = new ArrayList<>();
        List<String> careerElectiveCourses = List.of("음악", "미술", "체육");
        Map<String, List<Subject>> subjectMap = form.getGrade()
                .getSubjectListValue()
                .stream()
                .filter(subject -> !careerElectiveCourses.contains(subject.getSubjectName()))
                .collect(Collectors.groupingBy(
                        Subject::getSubjectName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        careerElectiveCourses.forEach(course -> {
            List<Subject> subjects = form.getGrade().getSubjectListValue()
                    .stream()
                    .filter(subject -> course.equals(subject.getSubjectName()))
                    .collect(Collectors.toList());
            if (!subjects.isEmpty()) {
                subjectMap.put(course, subjects);
            }
        });

        subjectMap.forEach((key, values) -> {
            SubjectVO subject = new SubjectVO(key);
            values.forEach(v -> {
                if (Objects.nonNull(v.getOriginalScore())) {
                    subject.score = v.getOriginalScore();
                } else {
                    String fieldName = "achievementLevel" + v.getGrade() + v.getSemester();
                    try {
                        SubjectVO.class.getField(fieldName)
                                .set(subject, v.getAchievementLevel());
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            value.add(subject);
        });

        return value;
    }

    private List<String> getRequiredTemplates(Form form) {
        if (form.getType().isSpecial()) {
            return List.of(
                    Templates.COVER,
                    form.getEducation().isQualificationExamination() ? Templates.QUALIFICATION_FORM : Templates.FORM,
                    Templates.DOCUMENT_ONE,
                    Templates.DOCUMENT_TWO,
                    Templates.RECOMMENDATION,
                    Templates.INFORMATION
            );
        } else if (form.getType().isSpecialAdmission()) {
            return List.of(
                    Templates.COVER,
                    form.getEducation().isQualificationExamination() ? Templates.QUALIFICATION_FORM : Templates.FORM,
                    Templates.DOCUMENT_ONE,
                    Templates.DOCUMENT_TWO,
                    Templates.CONFIRMATION,
                    Templates.INFORMATION
            );
        }

        return List.of(
                Templates.COVER,
                form.getEducation().isQualificationExamination() ? Templates.QUALIFICATION_FORM : Templates.FORM,
                Templates.DOCUMENT_ONE,
                Templates.DOCUMENT_TWO,
                Templates.INFORMATION
        );
    }
}

@Getter
class SubjectVO {

    private String subjectName;

    public AchievementLevel achievementLevel21;

    public AchievementLevel achievementLevel22;

    public AchievementLevel achievementLevel31;

    public AchievementLevel achievementLevel32;

    Integer score;

    public SubjectVO(String subjectName) {
        this.subjectName = subjectName;
    }
}