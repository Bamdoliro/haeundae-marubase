package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.type.GraduationType;
import com.bamdoliro.maru.domain.form.domain.value.School;
import com.bamdoliro.maru.domain.form.domain.value.Subject;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxService;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.constants.Schedule;
import com.bamdoliro.maru.shared.util.MathUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 입학(ENTERED) 처리된 신입생 명단을 학교 제출 서식(신입생 변환 파일)에 맞춰 내보낸다.
 * 시트1: 인적사항(연락처), 시트2~: 전형별 1단계 성적.
 */
@RequiredArgsConstructor
@UseCase
public class ExportNewStudentFormUseCase {

    private static final String FONT_NAME = "맑은 고딕";
    private static final byte[] HEADER_FILL_ARGB = {(byte) 0xFF, (byte) 0xDC, (byte) 0xE6, (byte) 0xF1};

    private static final int[][] SEMESTERS = {{2, 1}, {2, 2}, {3, 1}, {3, 2}};
    private static final String[] SUBJECTS = {"국어", "수학", "사회", "과학", "영어"};

    private static final int GRADE_HEADER_FIRST_ROW = 1;
    private static final int GRADE_HEADER_LAST_ROW = 4;
    private static final int GRADE_DATA_FIRST_ROW = 5;
    private static final int GRADE_LAST_COLUMN = 38; // AM

    private final FormFacade formFacade;
    private final XlsxService xlsxService;

    @Transactional(readOnly = true)
    public Resource execute() throws IOException {
        List<Form> formList = formFacade.getSortedFormList(FormStatus.ENTERED);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Styles styles = new Styles(workbook);

        writeContactSheet(workbook, styles, formList);
        writeGradeSheet(workbook, styles, "일반전형", filter(formList, FormType::isRegular), false);
        writeGradeSheet(workbook, styles, "기회균등전형", filter(formList, FormType::isEqualOpportunity), true);
        writeGradeSheet(workbook, styles, "사회통합전형", filter(formList, FormType::isSocietyDiversity), true);

        List<Form> supernumeraryFormList = filter(formList, FormType::isSupernumerary);
        if (!supernumeraryFormList.isEmpty()) {
            writeGradeSheet(workbook, styles, "정원 외 전형", supernumeraryFormList, true);
        }

        return xlsxService.convertToByteArrayResource(workbook);
    }

    private List<Form> filter(List<Form> formList, Predicate<FormType> predicate) {
        return formList.stream()
                .filter(form -> predicate.test(form.getType()))
                .toList();
    }

    // ---------------------------------------------------------------- 시트1: 인적사항(연락처)

    private void writeContactSheet(XSSFWorkbook workbook, Styles styles, List<Form> formList) {
        Sheet sheet = workbook.createSheet("신입생 인적사항(연락처)");

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(25.05f);
        setCell(titleRow, 0, Schedule.getAdmissionYear() + "학년도 신입생 인적사항(연락처)", styles.contactTitle);

        String[] headers = {"번호", "접수번호", "지원자", "전형구분", "출신학교", "핸드폰", "보호자", "보호자휴대폰", "비고"};
        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], styles.header);
        }

        int rowIndex = 2;
        for (Form form : formList) {
            Row row = sheet.createRow(rowIndex);
            setCell(row, 0, String.valueOf(rowIndex - 1), styles.data9);
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

        double[] widths = {8, 10, 25, 10, 20, 15.4, 10, 15.4, 10};
        for (int i = 0; i < widths.length; i++) {
            setColumnWidth(sheet, i, widths[i]);
        }
    }

    // ---------------------------------------------------------------- 시트2~: 전형별 1단계 성적

    private void writeGradeSheet(XSSFWorkbook workbook, Styles styles, String typeName, List<Form> formList, boolean wideTypeColumn) {
        Sheet sheet = workbook.createSheet(typeName + " 1단계 성적");

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(37.2f);
        setCell(titleRow, 0, typeName + " 지원자 성적", styles.gradeTitle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

        writeGradeHeader(sheet, styles);

        Map<Form, Integer> rankMap = calculateRank(formList);
        int rowIndex = GRADE_DATA_FIRST_ROW;
        for (Form form : formList) {
            writeGradeRow(sheet.createRow(rowIndex++), styles, form, rankMap.get(form));
        }

        setColumnWidth(sheet, 3, 10.1);
        setColumnWidth(sheet, 4, 8.8);
        setColumnWidth(sheet, 7, 8.8);
        setColumnWidth(sheet, 8, 10.7);
        setColumnWidth(sheet, 9, wideTypeColumn ? 15.2 : 8.8);
        setColumnWidth(sheet, 37, 8.8);
    }

    private void writeGradeHeader(Sheet sheet, Styles styles) {
        for (int r = GRADE_HEADER_FIRST_ROW; r <= GRADE_HEADER_LAST_ROW; r++) {
            Row row = sheet.createRow(r);
            for (int c = 0; c <= GRADE_LAST_COLUMN; c++) {
                setCell(row, c, null, styles.header);
            }
        }
        sheet.getRow(4).setHeightInPoints(18f);

        // A~L: 4행 병합 단일 헤더
        String[] personalHeaders = {"접수번호", "수험번호", "이름", "생년월일", "성별", "학력구분", "시도", "구군", "중학교명", "전형구분", "합불", "석차"};
        for (int c = 0; c < personalHeaders.length; c++) {
            headerCell(sheet, 1, c, personalHeaders[c]);
            merge(sheet, 1, 4, c, c);
        }

        headerCell(sheet, 1, 12, "중학교졸업(예정)");
        merge(sheet, 1, 1, 12, 37);

        headerCell(sheet, 2, 12, "교과성적");
        merge(sheet, 2, 2, 12, 32);
        headerCell(sheet, 2, 33, "출결감점");
        merge(sheet, 2, 2, 33, 37);

        String[] semesterHeaders = {"2학년1학기", "2학년2학기", "3학년1학기", "3학년2학기"};
        for (int s = 0; s < semesterHeaders.length; s++) {
            int firstColumn = 12 + s * SUBJECTS.length;
            headerCell(sheet, 3, firstColumn, semesterHeaders[s]);
            merge(sheet, 3, 3, firstColumn, firstColumn + SUBJECTS.length - 1);
            for (int i = 0; i < SUBJECTS.length; i++) {
                headerCell(sheet, 4, firstColumn + i, SUBJECTS[i]);
            }
        }

        headerCell(sheet, 3, 32, "교과성적");
        merge(sheet, 3, 4, 32, 32);
        headerCell(sheet, 3, 33, "미인정");
        merge(sheet, 3, 3, 33, 36);
        String[] attendanceHeaders = {"결석", "지각", "조퇴", "결과"};
        for (int i = 0; i < attendanceHeaders.length; i++) {
            headerCell(sheet, 4, 33 + i, attendanceHeaders[i]);
        }
        headerCell(sheet, 3, 37, "출결감점");
        merge(sheet, 3, 4, 37, 37);

        headerCell(sheet, 1, GRADE_LAST_COLUMN, "1단계성적");
        merge(sheet, 1, 4, GRADE_LAST_COLUMN, GRADE_LAST_COLUMN);

        // 헤더 블록 바깥 테두리는 굵게(medium)
        CellRangeAddress outline = new CellRangeAddress(GRADE_HEADER_FIRST_ROW, GRADE_HEADER_LAST_ROW, 0, GRADE_LAST_COLUMN);
        RegionUtil.setBorderTop(BorderStyle.MEDIUM, outline, sheet);
        RegionUtil.setBorderBottom(BorderStyle.MEDIUM, outline, sheet);
        RegionUtil.setBorderLeft(BorderStyle.MEDIUM, outline, sheet);
        RegionUtil.setBorderRight(BorderStyle.MEDIUM, outline, sheet);
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
        setCell(row, 10, Boolean.TRUE.equals(form.isPassed()) ? "합격" : "불합격", styles.data9);
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

    /**
     * 학교 주소("부산광역시 해운대구 ...")의 두 번째 토큰을 구/군으로 사용한다.
     * 구/군/시로 끝나지 않으면(세종시 등) 비워 둔다.
     */
    private String schoolDistrict(Form form) {
        School school = form.getEducation().getSchool();
        if (school == null || school.getAddress() == null) {
            return null;
        }
        String[] tokens = school.getAddress().trim().split("\\s+");
        if (tokens.length < 2) {
            return null;
        }
        String district = tokens[1];
        if (district.endsWith("구") || district.endsWith("군") || district.endsWith("시")) {
            return district;
        }
        return null;
    }

    // ---------------------------------------------------------------- 셀/스타일 헬퍼

    private void headerCell(Sheet sheet, int rowIndex, int columnIndex, String value) {
        sheet.getRow(rowIndex).getCell(columnIndex).setCellValue(value);
    }

    private void merge(Sheet sheet, int firstRow, int lastRow, int firstColumn, int lastColumn) {
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn));
    }

    private void setColumnWidth(Sheet sheet, int columnIndex, double width) {
        sheet.setColumnWidth(columnIndex, (int) Math.round(width * 256));
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

    private static class Styles {
        private final CellStyle contactTitle;
        private final CellStyle gradeTitle;
        private final CellStyle header;
        private final CellStyle data9;
        private final CellStyle data10;
        private final CellStyle data11;

        Styles(XSSFWorkbook workbook) {
            workbook.getFontAt(0).setFontName(FONT_NAME);

            contactTitle = titleStyle(workbook, false);
            gradeTitle = titleStyle(workbook, true);

            XSSFCellStyle headerStyle = borderedStyle(workbook, 10);
            headerStyle.setFillForegroundColor(new XSSFColor(HEADER_FILL_ARGB, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header = headerStyle;

            data9 = borderedStyle(workbook, 9);
            data10 = borderedStyle(workbook, 10);
            data11 = borderedStyle(workbook, 11);
        }

        private static CellStyle titleStyle(XSSFWorkbook workbook, boolean bold) {
            CellStyle style = workbook.createCellStyle();
            style.setFont(font(workbook, 16, bold));
            style.setAlignment(HorizontalAlignment.LEFT);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }

        private static XSSFCellStyle borderedStyle(XSSFWorkbook workbook, int fontSize) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font(workbook, fontSize, false));
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }

        private static Font font(XSSFWorkbook workbook, int size, boolean bold) {
            Font font = workbook.createFont();
            font.setFontName(FONT_NAME);
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            return font;
        }
    }
}
