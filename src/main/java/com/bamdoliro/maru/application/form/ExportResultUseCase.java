package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxGenerator;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.util.MathUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
@UseCase
public class ExportResultUseCase {

    private final FormFacade formFacade;
    private final XlsxGenerator xlsxGenerator;

    public Resource execute() throws IOException {
        List<Form> formList = formFacade.getSortedFormList(null);

        List<Function<Form, Object>> columnList = List.of(
                Form::getId,
                Form::getExaminationNumber,
                form -> form.getOriginalType().getDescription(),
                form -> form.getType().getDescription(),
                form -> form.getStatus().getDescription(),
                form -> form.getApplicant().getName(),
                form -> form.getApplicant().getGender().getDescription(),
                form -> form.getApplicant().getBirthday().format(DateTimeFormatter.BASIC_ISO_DATE),
                form -> form.getEducation().getSchool().getLocation(),
                form -> form.getEducation().getGraduationTypeToString(),
                form -> form.getEducation().getSchool().getName(),
                form -> form.getEducation().getSchool().getCode(),
                form -> form.getGrade().getSubjectList().getSubjectMap().getSubjectListOf(2, 1).size(),
                form -> form.getGrade().getSubjectList().getSubjectMap().getSubjectListOf(2, 1).totalScore(),
                form -> form.getGrade().getSubjectList().getSubjectMap().getSubjectListOf(2, 2).size(),
                form -> form.getGrade().getSubjectList().getSubjectMap().getSubjectListOf(2, 2).totalScore(),
                form -> form.getGrade().getSubjectList().getSubjectMap().getSubjectListOf(3, 1).size(),
                form -> form.getGrade().getSubjectList().getSubjectMap().getSubjectListOf(3, 1).totalScore(),
                form -> MathUtil.roundTo(form.getScore().getSubjectGradeScore(), 3),
                form -> form.getScore().getAttendanceScore(),
                form -> form.getScore().getSelfDirectedScore(),
                form -> form.getScore().getPersonalityScore(),
                form -> form.tookSecondRound() ? MathUtil.roundTo(form.getScore().getTotalScore(), 3) : MathUtil.roundTo(form.getScore().getFirstRoundScore(), 3)
        );

        List<String> styleList = List.of(
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "default",
                "right",
                "right",
                "right",
                "right",
                "right",
                "right",
                "right",
                "right"
        );

        return xlsxGenerator.execute("전체결과", formList, columnList, styleList);
    }
}
