package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.value.School;
import com.bamdoliro.maru.domain.form.domain.value.Score;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxService;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.util.MathUtil;
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
import java.util.List;
import java.util.function.Function;

/**
 * 전체 원서의 1차 점수(교과성적, 총점)를 엑셀로 내보낸다.
 * 점수나 학교 정보가 없는 원서는 해당 칸을 비운다.
 */
@RequiredArgsConstructor
@UseCase
public class ExportFirstScoreUseCase {

    private static final List<String> HEADERS = List.of(
            "ID", "수험번호", "원래 전형", "현재 전형", "상태", "이름", "성별", "생년월일",
            "출신지역", "학적구분", "학교명", "학교코드", "교과성적", "총점"
    );

    private static final List<Function<Form, Object>> COLUMNS = List.of(
            Form::getId,
            Form::getExaminationNumber,
            form -> form.getOriginalType().getDescription(),
            form -> form.getType().getDescription(),
            form -> form.getStatus().getDescription(),
            form -> form.getApplicant().getName(),
            form -> form.getApplicant().getGender().getDescription(),
            form -> form.getApplicant().getBirthday().format(DateTimeFormatter.BASIC_ISO_DATE),
            form -> school(form, School::getLocation),
            form -> form.getEducation().getGraduationTypeToString(),
            form -> school(form, School::getName),
            form -> school(form, School::getCode),
            form -> score(form, Score::getSubjectGradeScore),
            form -> score(form, form.tookSecondRound() ? Score::getTotalScore : Score::getFirstRoundScore)
    );

    private final FormFacade formFacade;
    private final XlsxService xlsxService;

    @Transactional(readOnly = true)
    public Resource execute() throws IOException {
        List<Form> formList = formFacade.getSortedFormList(null);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("1차원서점수");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle defaultStyle = xlsxService.createDefaultCellStyle(workbook);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.size(); i++) {
            setCell(headerRow, i, HEADERS.get(i), headerStyle);
        }

        int rowIndex = 1;
        for (Form form : formList) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < COLUMNS.size(); i++) {
                setCell(row, i, COLUMNS.get(i).apply(form), defaultStyle);
            }
        }

        for (int i = 0; i < HEADERS.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        return xlsxService.convertToByteArrayResource(workbook);
    }

    private static Object school(Form form, Function<School, String> getter) {
        School school = form.getEducation().getSchool();
        return school == null ? null : getter.apply(school);
    }

    private static Object score(Form form, Function<Score, Double> getter) {
        Score score = form.getScore();
        if (score == null) {
            return null;
        }
        Double value = getter.apply(score);
        return value == null ? null : MathUtil.roundTo(value, 3);
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
}
