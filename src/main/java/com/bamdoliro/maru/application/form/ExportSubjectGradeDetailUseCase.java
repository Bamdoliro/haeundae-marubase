package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.value.School;
import com.bamdoliro.maru.domain.form.domain.value.Subject;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxService;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 최종 합격자의 학년/학기/과목별 성취수준을 엑셀로 내보낸다.
 * 과목 열은 합격자들이 가진 (학년, 학기, 과목) 조합을 처음 나온 순서대로 모아 만들고,
 * 해당 조합이 없는 학생은 빈 칸으로 둔다.
 */
@RequiredArgsConstructor
@UseCase
public class ExportSubjectGradeDetailUseCase {

    private static final List<String> FIXED_HEADERS = List.of("ID", "수험번호", "이름", "전화번호", "생년월일", "학교명");

    private final FormFacade formFacade;
    private final XlsxService xlsxService;

    @Transactional(readOnly = true)
    public Resource execute() throws IOException {
        List<Form> formList = formFacade.getSortedFormList(FormStatus.PASSED);
        List<SubjectKey> subjectKeys = List.copyOf(extractSubjectKeys(formList));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("합격자성적상세");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle defaultStyle = xlsxService.createDefaultCellStyle(workbook);

        Row headerRow = sheet.createRow(0);
        int column = 0;
        for (String header : FIXED_HEADERS) {
            setCell(headerRow, column++, header, headerStyle);
        }
        for (SubjectKey key : subjectKeys) {
            setCell(headerRow, column++, key.header(), headerStyle);
        }

        int rowIndex = 1;
        for (Form form : formList) {
            Row row = sheet.createRow(rowIndex++);
            column = 0;
            setCell(row, column++, form.getId(), defaultStyle);
            setCell(row, column++, form.getExaminationNumber(), defaultStyle);
            setCell(row, column++, form.getApplicant().getName(), defaultStyle);
            setCell(row, column++, form.getApplicant().getPhoneNumber().toString(), defaultStyle);
            setCell(row, column++, form.getApplicant().getBirthday().format(DateTimeFormatter.BASIC_ISO_DATE), defaultStyle);
            setCell(row, column++, schoolName(form), defaultStyle);

            List<Subject> subjects = form.getGrade().getSubjectListValue();
            for (SubjectKey key : subjectKeys) {
                setCell(row, column++, achievementLevel(subjects, key), defaultStyle);
            }
        }

        return xlsxService.convertToByteArrayResource(workbook);
    }

    private Set<SubjectKey> extractSubjectKeys(List<Form> formList) {
        Set<SubjectKey> keys = new LinkedHashSet<>();
        for (Form form : formList) {
            for (Subject subject : form.getGrade().getSubjectListValue()) {
                keys.add(new SubjectKey(subject.getGrade(), subject.getSemester(), subject.getSubjectName()));
            }
        }
        return keys;
    }

    private String achievementLevel(List<Subject> subjects, SubjectKey key) {
        return subjects.stream()
                .filter(subject -> key.matches(subject))
                .findFirst()
                .map(subject -> subject.getAchievementLevel().getDescription())
                .orElse(null);
    }

    private String schoolName(Form form) {
        School school = form.getEducation().getSchool();
        return school == null ? null : school.getName();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = xlsxService.createDefaultCellStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void setCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 학년 + 학기 + 과목명. 검정고시 과목은 학년/학기가 0으로 저장된다.
     */
    private record SubjectKey(Integer grade, Integer semester, String subjectName) {

        boolean matches(Subject subject) {
            return Objects.equals(grade, subject.getGrade())
                    && Objects.equals(semester, subject.getSemester())
                    && Objects.equals(subjectName, subject.getSubjectName());
        }

        String header() {
            boolean qualificationExamination = Objects.equals(grade, 0) && Objects.equals(semester, 0);
            if (qualificationExamination || grade == null || semester == null) {
                return "검정고시 " + subjectName;
            }
            return String.format("%d학년 %d학기 %s", grade, semester, subjectName);
        }
    }
}
