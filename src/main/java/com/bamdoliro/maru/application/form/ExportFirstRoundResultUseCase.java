package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.type.GraduationType;
import com.bamdoliro.maru.domain.form.domain.value.School;
import com.bamdoliro.maru.domain.form.domain.value.Subject;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxService;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.constants.Schedule;
import com.bamdoliro.maru.shared.util.MathUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 1차(서류) 전형 결과를 학교 제출 서식(templates/xlsx/1차전형결과.xlsx)에 채워 내보낸다.
 * 시트1: 1차 합격자 인적사항(연락처) - 1차 합격자만
 * 시트2~4: 일반전형/기회균등전형/사회통합전형 1단계 성적 - 1차 합격자 + 불합격자 전체
 * 헤더, 병합, 제목은 템플릿에 있고 여기서는 데이터 행만 채운다.
 */
@RequiredArgsConstructor
@UseCase
public class ExportFirstRoundResultUseCase {

    private static final String FONT_NAME = "맑은 고딕";

    private static final int CONTACT_SHEET = 0;
    private static final int REGULAR_SHEET = 1;
    private static final int EQUAL_OPPORTUNITY_SHEET = 2;
    private static final int SOCIETY_DIVERSITY_SHEET = 3;

    private static final int CONTACT_DATA_FIRST_ROW = 2;
    private static final int GRADE_DATA_FIRST_ROW = 5;

    private static final String SUPERNUMERARY_SHEET_NAME = "정원 외 전형 1단계 성적";
    private static final String SUPERNUMERARY_SHEET_TITLE = "정원 외 전형 지원자 성적";

    private static final int[][] SEMESTERS = {{2, 1}, {2, 2}, {3, 1}, {3, 2}};
    private static final String[] SUBJECTS = {"국어", "수학", "사회", "과학", "영어"};

    private final FormRepository formRepository;
    private final XlsxService xlsxService;

    @Transactional(readOnly = true)
    public Resource execute() throws IOException {
        List<Form> formList = formRepository.findFirstRoundForm()
                .stream()
                .sorted(Comparator.comparing(Form::getExaminationNumber))
                .toList();

        Workbook workbook = xlsxService.openTemplate("1차전형결과");
        Styles styles = new Styles(workbook);

        // 정원 외 전형 시트는 템플릿에 없으므로, 대상자가 있을 때만 일반전형 시트를 복제해서 만든다.
        // 데이터를 채우기 전에 복제해야 빈 헤더만 복사된다.
        List<Form> supernumeraryFormList = filter(formList, FormType::isSupernumerary);
        Sheet supernumerarySheet = null;
        if (!supernumeraryFormList.isEmpty()) {
            supernumerarySheet = workbook.cloneSheet(REGULAR_SHEET);
            workbook.setSheetName(workbook.getSheetIndex(supernumerarySheet), SUPERNUMERARY_SHEET_NAME);
            replaceText(supernumerarySheet, 0, 0, SUPERNUMERARY_SHEET_TITLE);
        }

        List<Form> firstPassedFormList = formList.stream()
                .filter(form -> Boolean.TRUE.equals(form.isFirstPassed()))
                .toList();
        writeContactSheet(workbook.getSheetAt(CONTACT_SHEET), styles, firstPassedFormList);

        writeGradeSheet(workbook.getSheetAt(REGULAR_SHEET), styles, filter(formList, FormType::isRegular));
        writeGradeSheet(workbook.getSheetAt(EQUAL_OPPORTUNITY_SHEET), styles, filter(formList, FormType::isEqualOpportunity));
        writeGradeSheet(workbook.getSheetAt(SOCIETY_DIVERSITY_SHEET), styles, filter(formList, FormType::isSocietyDiversity));
        if (supernumerarySheet != null) {
            writeGradeSheet(supernumerarySheet, styles, supernumeraryFormList);
        }

        return xlsxService.convertToByteArrayResource(workbook);
    }

    private List<Form> filter(List<Form> formList, Predicate<FormType> predicate) {
        return formList.stream()
                .filter(form -> predicate.test(form.getType()))
                .toList();
    }

    // ---------------------------------------------------------------- 시트1: 1차 합격자 인적사항(연락처)

    private void writeContactSheet(Sheet sheet, Styles styles, List<Form> formList) {
        // 제목 서식은 템플릿 그대로 두고, 연도만 입학 일정 기준으로 맞춘다.
        replaceText(sheet, 0, 0, Schedule.getAdmissionYear() + "학년도 1차 합격자 인적사항(연락처)");

        int rowIndex = CONTACT_DATA_FIRST_ROW;
        for (Form form : formList) {
            Row row = sheet.createRow(rowIndex);
            setCell(row, 0, String.valueOf(rowIndex - CONTACT_DATA_FIRST_ROW + 1), styles.data9);
            setCell(row, 1, String.valueOf(form.getExaminationNumber()), styles.data9);
            setCell(row, 2, form.getApplicant().getName(), styles.data11);
            setCell(row, 3, form.getType().getSubCategory().getDescription(), styles.data9);
            setCell(row, 4, schoolName(form), styles.data11);
            setCell(row, 5, form.getApplicant().getPhoneNumber().toString(), styles.data10);
            setCell(row, 6, form.getParent().getName(), styles.data10);
            setCell(row, 7, form.getParent().getPhoneNumber().toString(), styles.data10);
            setCell(row, 8, null, styles.data9);
            rowIndex++;
        }
    }

    // ---------------------------------------------------------------- 시트2~: 전형별 1단계 성적

    private void writeGradeSheet(Sheet sheet, Styles styles, List<Form> formList) {
        Map<Form, Integer> rankMap = calculateRank(formList);
        int rowIndex = GRADE_DATA_FIRST_ROW;
        for (Form form : formList) {
            writeGradeRow(sheet.createRow(rowIndex++), styles, form, rankMap.get(form));
        }
    }

    private void writeGradeRow(Row row, Styles styles, Form form, Integer rank) {
        setCell(row, 0, form.getExaminationNumber(), styles.data11);
        setCell(row, 1, form.getExaminationNumber(), styles.data11);
        setCell(row, 2, form.getApplicant().getName(), styles.data11);
        setCell(row, 3, Long.parseLong(form.getApplicant().getBirthday().format(DateTimeFormatter.BASIC_ISO_DATE)), styles.data11);
        setCell(row, 4, form.getApplicant().getGender().getDescription(), styles.data9);
        setCell(row, 5, educationType(form.getEducation().getGraduationType()), styles.data9);
        setCell(row, 6, schoolLocation(form), styles.data9);
        setCell(row, 7, schoolDistrict(form), styles.data9);
        setCell(row, 8, schoolName(form), styles.data9);
        setCell(row, 9, form.getType().getDescription(), styles.data9);
        setCell(row, 10, Boolean.TRUE.equals(form.isFirstPassed()) ? "합격" : "불합격", styles.data9);
        setCell(row, 11, rank, styles.data9);

        List<Subject> subjectList = form.getGrade().getSubjectListValue();
        int column = 12;
        for (int[] semester : SEMESTERS) {
            for (String subjectName : SUBJECTS) {
                setCell(row, column++, achievementLevel(subjectList, semester[0], semester[1], subjectName), styles.data9);
            }
        }

        setCell(row, 32, MathUtil.roundTo(form.getScore().getSubjectGradeScore(), 3), styles.data11);
        setCell(row, 33, form.getGrade().getTotalAbsenceCount(), styles.data11);
        setCell(row, 34, form.getGrade().getTotalLatenessCount(), styles.data11);
        setCell(row, 35, form.getGrade().getTotalEarlyLeaveCount(), styles.data11);
        setCell(row, 36, form.getGrade().getTotalClassAbsenceCount(), styles.data11);
        setCell(row, 37, form.getScore().getAttendanceScore(), styles.data11);
        setCell(row, 38, MathUtil.roundTo(form.getScore().getFirstRoundScore(), 3), styles.data11);
    }

    // ---------------------------------------------------------------- 값 변환

    /**
     * 시트 내 1단계 성적 내림차순 석차. 동점자는 같은 석차, 다음 석차는 동점자 수만큼 건너뛴다(1, 2, 2, 4).
     */
    private Map<Form, Integer> calculateRank(List<Form> formList) {
        List<Double> sortedScores = formList.stream()
                .map(form -> MathUtil.roundTo(form.getScore().getFirstRoundScore(), 3))
                .sorted((a, b) -> Double.compare(b, a))
                .toList();

        Map<Form, Integer> rankMap = new HashMap<>();
        for (Form form : formList) {
            double score = MathUtil.roundTo(form.getScore().getFirstRoundScore(), 3);
            rankMap.put(form, sortedScores.indexOf(score) + 1);
        }
        return rankMap;
    }

    private String educationType(GraduationType graduationType) {
        if (graduationType == GraduationType.QUALIFICATION_EXAMINATION) {
            return graduationType.getDescription().replace(" ", "");
        }
        return "중학교" + graduationType.getDescription().replace(" ", "");
    }

    private String achievementLevel(List<Subject> subjectList, int grade, int semester, String subjectName) {
        return subjectList.stream()
                .filter(subject -> Objects.equals(subject.getGrade(), grade)
                        && Objects.equals(subject.getSemester(), semester)
                        && subjectName.equals(subject.getSubjectName()))
                .findFirst()
                .map(subject -> subject.getAchievementLevel().getDescription())
                .orElse(null);
    }

    private String schoolName(Form form) {
        School school = form.getEducation().getSchool();
        return school == null ? null : school.getName();
    }

    private String schoolLocation(Form form) {
        School school = form.getEducation().getSchool();
        return school == null ? null : school.getLocation();
    }

    private String schoolDistrict(Form form) {
        School school = form.getEducation().getSchool();
        return school == null ? null : extractDistrict(school.getAddress());
    }

    /**
     * 학교 도로명 주소(NEIS ORG_RDNMA)에서 시/군/구를 뽑는다.
     * - "부산광역시 해운대구 ..." -> "해운대구", "경상남도 거제시 ..." -> "거제시"
     * - 구가 있는 시는 함께 붙인다: "경상남도 창원시 의창구 ..." -> "창원시 의창구"
     * - 시/군/구 단위가 없으면(세종특별자치시 등) 비워 둔다.
     */
    static String extractDistrict(String address) {
        if (address == null) {
            return null;
        }
        String[] tokens = address.trim().split("\\s+");
        if (tokens.length < 2) {
            return null;
        }
        String district = tokens[1];
        if (district.endsWith("시") && tokens.length > 2 && tokens[2].endsWith("구")) {
            return district + " " + tokens[2];
        }
        if (district.endsWith("구") || district.endsWith("군") || district.endsWith("시")) {
            return district;
        }
        return null;
    }

    // ---------------------------------------------------------------- 셀/스타일 헬퍼

    /**
     * 템플릿 셀의 글자를 바꾼다. 템플릿은 인라인 문자열(t="inlineStr")로 저장되어 있는데,
     * POI의 setCellValue는 기존 인라인 문자열을 지우지 않아 엑셀에 이전 글자가 그대로 보인다.
     * 그래서 셀을 지우고 같은 서식으로 다시 만든다.
     */
    private void replaceText(Sheet sheet, int rowIndex, int columnIndex, String text) {
        Row row = sheet.getRow(rowIndex);
        Cell oldCell = row.getCell(columnIndex);
        CellStyle style = oldCell.getCellStyle();
        row.removeCell(oldCell);

        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        cell.setCellValue(text);
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
     * 템플릿에는 데이터 행 서식이 없어서, 채워 넣는 셀에 샘플과 같은 서식(가는 테두리, 가운데 정렬, 맑은 고딕)을 입힌다.
     */
    private static class Styles {
        private final CellStyle data9;
        private final CellStyle data10;
        private final CellStyle data11;

        Styles(Workbook workbook) {
            data9 = dataStyle(workbook, 9);
            data10 = dataStyle(workbook, 10);
            data11 = dataStyle(workbook, 11);
        }

        private static CellStyle dataStyle(Workbook workbook, int fontSize) {
            Font font = workbook.createFont();
            font.setFontName(FONT_NAME);
            font.setFontHeightInPoints((short) fontSize);

            CellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
