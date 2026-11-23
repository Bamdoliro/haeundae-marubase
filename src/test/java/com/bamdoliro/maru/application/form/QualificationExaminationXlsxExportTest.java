package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.type.GraduationType;
import com.bamdoliro.maru.domain.form.domain.value.Attendance;
import com.bamdoliro.maru.domain.form.domain.value.Education;
import com.bamdoliro.maru.domain.form.domain.value.Grade;
import com.bamdoliro.maru.domain.form.domain.value.School;
import com.bamdoliro.maru.domain.form.domain.value.Subject;
import com.bamdoliro.maru.domain.form.domain.value.SubjectList;
import com.bamdoliro.maru.domain.form.domain.value.Teacher;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxGenerator;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxService;
import com.bamdoliro.maru.infrastructure.xlsx.constant.XlsxConstant;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 검정고시 과목은 학년/학기 0으로 저장된다. 학기별 칸이 고정된 엑셀에서는
 * 학기 구분이 없는 검정고시 성적을 4개 학기 칸(2-1, 2-2, 3-1, 3-2)에 똑같이 채우는지 실제 워크북으로 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class QualificationExaminationXlsxExportTest {

    private static final String[] SEMESTERS = {"2-1", "2-2", "3-1", "3-2"};

    // 1차전형결과.xlsx 전형별 성적 시트
    private static final int REGULAR_SHEET = 1;
    private static final int GRADE_DATA_FIRST_ROW = 5;
    private static final int FIRST_ACHIEVEMENT_COLUMN = 12;
    private static final int LAST_ACHIEVEMENT_COLUMN = 31;

    // 전체결과 엑셀: 학기별 (과목 수, 성취도 점수 합계) 컬럼
    private static final int RESULT_FIRST_ROW = XlsxConstant.FIRST_ROW_INDEX_WITH_TITLE;
    private static final int FIRST_SEMESTER_COUNT_COLUMN = 12;

    @Mock
    private FormRepository formRepository;

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    private ExportFirstRoundResultUseCase exportFirstRoundResultUseCase;
    private ExportResultUseCase exportResultUseCase;

    @BeforeEach
    void setUp() {
        XlsxService xlsxService = new XlsxService();
        exportFirstRoundResultUseCase = new ExportFirstRoundResultUseCase(formRepository, xlsxService, admissionScheduleFacade);
        exportResultUseCase = new ExportResultUseCase(new FormFacade(formRepository), new XlsxGenerator(xlsxService));
    }

    @Test
    void 일차전형결과_검정고시_학생은_환산_성취도를_4개_학기_칸에_모두_채운다() throws Exception {
        given(formRepository.findFirstRoundForm()).willReturn(List.of(regularForm(1001L), qualificationExaminationForm(1002L)));
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2031, true));

        try (Workbook workbook = WorkbookFactory.create(exportFirstRoundResultUseCase.execute().getInputStream())) {
            Sheet sheet = workbook.getSheetAt(REGULAR_SHEET);
            Row regular = sheet.getRow(GRADE_DATA_FIRST_ROW);
            Row qualificationExamination = sheet.getRow(GRADE_DATA_FIRST_ROW + 1);
            assertThat(regular.getCell(0).getNumericCellValue()).isEqualTo(1001);
            assertThat(qualificationExamination.getCell(0).getNumericCellValue()).isEqualTo(1002);
            assertThat(qualificationExamination.getCell(5).getStringCellValue()).isEqualTo("고입검정");

            // 국어 100(A), 수학 98(B), 사회 95(C), 과학 90(D), 영어 80(E)로 환산된 값이 학기마다 반복된다.
            List<String> expected = new ArrayList<>();
            for (String ignored : SEMESTERS) {
                expected.addAll(List.of("A", "B", "C", "D", "E"));
            }
            assertThat(achievementLevels(qualificationExamination)).containsExactlyElementsOf(expected);

            // 일반 학생은 기존처럼 학년/학기별 성취도가 그대로 나간다(3학년 수학만 B).
            assertThat(achievementLevels(regular)).containsExactly(
                    "A", "A", "A", "A", "A",
                    "A", "A", "A", "A", "A",
                    "A", "B", "A", "A", "A",
                    "A", "B", "A", "A", "A"
            );
        }
    }

    @Test
    void 전체결과_검정고시_학생은_학기별_과목_수와_성취도_합계를_4개_학기_칸에_모두_채운다() throws Exception {
        given(formRepository.findByStatus(null)).willReturn(List.of(regularForm(1001L), qualificationExaminationForm(1002L)));

        try (Workbook workbook = WorkbookFactory.create(exportResultUseCase.execute().getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row regular = findRow(sheet, 1001L);
            Row qualificationExamination = findRow(sheet, 1002L);

            // 성취도 점수 A=40, B=32, C=24, D=16, E=8 -> 합계 120
            for (int i = 0; i < SEMESTERS.length; i++) {
                int countColumn = FIRST_SEMESTER_COUNT_COLUMN + i * 2;
                assertThat(qualificationExamination.getCell(countColumn).getNumericCellValue())
                        .as("%s 과목 수", SEMESTERS[i]).isEqualTo(5);
                assertThat(qualificationExamination.getCell(countColumn + 1).getNumericCellValue())
                        .as("%s 성취도 합계", SEMESTERS[i]).isEqualTo(120);
            }

            // 일반 학생: 2학년은 모두 A(200), 3학년은 수학만 B(192)
            int[] regularTotals = {200, 200, 192, 192};
            for (int i = 0; i < SEMESTERS.length; i++) {
                int countColumn = FIRST_SEMESTER_COUNT_COLUMN + i * 2;
                assertThat(regular.getCell(countColumn).getNumericCellValue()).isEqualTo(5);
                assertThat(regular.getCell(countColumn + 1).getNumericCellValue()).isEqualTo(regularTotals[i]);
            }
        }
    }

    private List<String> achievementLevels(Row row) {
        List<String> values = new ArrayList<>();
        for (int column = FIRST_ACHIEVEMENT_COLUMN; column <= LAST_ACHIEVEMENT_COLUMN; column++) {
            Cell cell = row.getCell(column);
            values.add(cell == null ? null : cell.getStringCellValue());
        }
        return values;
    }

    private Row findRow(Sheet sheet, Long examinationNumber) {
        for (int r = RESULT_FIRST_ROW; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null && row.getCell(1) != null && row.getCell(1).getNumericCellValue() == examinationNumber) {
                return row;
            }
        }
        throw new AssertionError("수험번호 " + examinationNumber + " 행이 없습니다.");
    }

    private Form regularForm(Long examinationNumber) {
        Form form = FormFixture.createForm(FormType.REGULAR);
        ReflectionTestUtils.setField(form, "id", examinationNumber);
        ReflectionTestUtils.setField(form, "examinationNumber", examinationNumber);
        ReflectionTestUtils.setField(form.getEducation(), "graduationDate", "2021-02-15");
        return form;
    }

    private Form qualificationExaminationForm(Long examinationNumber) {
        Form form = regularForm(examinationNumber);
        ReflectionTestUtils.setField(form, "education", new Education(
                GraduationType.QUALIFICATION_EXAMINATION,
                "2021-03-25",
                new School(null, null, null, null),
                new Teacher(null, null, null)
        ));
        ReflectionTestUtils.setField(form, "grade", new Grade(
                new SubjectList(List.of(
                        new Subject("국어", 100),
                        new Subject("수학", 98),
                        new Subject("사회", 95),
                        new Subject("과학", 90),
                        new Subject("영어", 80)
                )),
                new Attendance(0, 0, 0, 0),
                new Attendance(0, 0, 0, 0),
                new Attendance(0, 0, 0, 0)
        ));
        return form;
    }
}
