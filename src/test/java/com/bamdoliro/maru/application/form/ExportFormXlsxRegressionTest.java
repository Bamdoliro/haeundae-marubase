package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxGenerator;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxService;
import com.bamdoliro.maru.infrastructure.xlsx.constant.XlsxConstant;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExportFormXlsxRegressionTest {

    private static final int FIRST_ROW = XlsxConstant.FIRST_ROW_INDEX_WITH_TITLE;

    // 최종합격자 엑셀 컬럼 위치
    private static final int FINAL_EXAMINATION_NUMBER = 1;
    private static final int FINAL_TYPE = 4;
    private static final int FINAL_STATUS = 5;
    private static final int FINAL_BIRTHDAY = 9;

    // 2차전형결과 엑셀 컬럼 위치
    private static final int SECOND_ROUND_BIRTHDAY = 8;

    @Mock
    private FormRepository formRepository;

    private ExportFinalPassedFormUseCase exportFinalPassedFormUseCase;
    private ExportSecondRoundResultUseCase exportSecondRoundResultUseCase;

    @BeforeEach
    void setUp() {
        FormFacade formFacade = new FormFacade(formRepository);
        XlsxGenerator generator = new XlsxGenerator(new XlsxService());
        exportFinalPassedFormUseCase = new ExportFinalPassedFormUseCase(formFacade, formRepository, generator);
        exportSecondRoundResultUseCase = new ExportSecondRoundResultUseCase(formRepository, formFacade, generator);
    }

    @Test
    void 이차전형_생년월일은_일련번호가_아닌_날짜_문자열로_내보낸다() throws Exception {
        given(formRepository.findSecondRoundForm()).willReturn(List.of(form(1001L, FormType.REGULAR, FormStatus.PASSED)));

        try (Workbook workbook = WorkbookFactory.create(exportSecondRoundResultUseCase.execute().getInputStream())) {
            Cell birthday = workbook.getSheetAt(0).getRow(FIRST_ROW).getCell(SECOND_ROUND_BIRTHDAY);
            assertThat(birthday.getCellType()).isEqualTo(CellType.STRING);
            assertThat(new DataFormatter().formatCellValue(birthday)).isEqualTo("20050415");
        }
    }

    @Test
    void 최종합격자_생년월일은_날짜_문자열로_내보낸다() throws Exception {
        given(formRepository.findByStatusIn(Set.of(FormStatus.PASSED, FormStatus.ENTERED)))
                .willReturn(List.of(form(1001L, FormType.REGULAR, FormStatus.PASSED)));

        try (Workbook workbook = WorkbookFactory.create(exportFinalPassedFormUseCase.execute().getInputStream())) {
            Cell birthday = workbook.getSheetAt(0).getRow(FIRST_ROW).getCell(FINAL_BIRTHDAY);
            assertThat(birthday.getCellType()).isEqualTo(CellType.STRING);
            assertThat(birthday.getStringCellValue()).isEqualTo("20050415");
        }
    }

    @Test
    void 입학_처리_전에도_최종합격자를_내보낸다() throws Exception {
        given(formRepository.findByStatusIn(Set.of(FormStatus.PASSED, FormStatus.ENTERED)))
                .willReturn(List.of(form(1001L, FormType.REGULAR, FormStatus.PASSED)));

        try (Workbook workbook = WorkbookFactory.create(exportFinalPassedFormUseCase.execute().getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(FIRST_ROW).getCell(FINAL_EXAMINATION_NUMBER).getNumericCellValue())
                    .isEqualTo(1001);
            assertThat(sheet.getRow(FIRST_ROW).getCell(FINAL_STATUS).getStringCellValue())
                    .isEqualTo(FormStatus.PASSED.getDescription());
        }
    }

    @Test
    void 최종합격과_입학만_포함하고_기존_수험번호_정렬을_유지한다() throws Exception {
        List<Form> forms = List.of(
                form(1003L, FormType.REGULAR, FormStatus.ENTERED),
                form(1002L, FormType.REGULAR, FormStatus.FAILED),
                form(1001L, FormType.REGULAR, FormStatus.PASSED),
                form(1004L, FormType.REGULAR, FormStatus.FIRST_PASSED),
                form(1005L, FormType.REGULAR, FormStatus.APPROVED)
        );
        given(formRepository.findByStatusIn(anyCollection())).willAnswer(invocation -> {
            Collection<FormStatus> statuses = invocation.getArgument(0);
            return forms.stream().filter(form -> statuses.contains(form.getStatus())).toList();
        });

        try (Workbook workbook = WorkbookFactory.create(exportFinalPassedFormUseCase.execute().getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(FIRST_ROW + 1);
            assertThat(sheet.getRow(FIRST_ROW).getCell(FINAL_EXAMINATION_NUMBER).getNumericCellValue()).isEqualTo(1001);
            assertThat(sheet.getRow(FIRST_ROW + 1).getCell(FINAL_EXAMINATION_NUMBER).getNumericCellValue()).isEqualTo(1003);
        }
        verify(formRepository).findByStatusIn(Set.of(FormStatus.PASSED, FormStatus.ENTERED));
    }

    @Test
    void 최종합격자는_전형별로_묶은_뒤_수험번호_순으로_정렬한다() throws Exception {
        given(formRepository.findByStatusIn(Set.of(FormStatus.PASSED, FormStatus.ENTERED))).willReturn(List.of(
                form(3001L, FormType.NATIONAL_VETERANS_EDUCATION, FormStatus.ENTERED),
                form(2002L, FormType.MULTICULTURAL, FormStatus.PASSED),
                form(1002L, FormType.REGULAR, FormStatus.ENTERED),
                form(2001L, FormType.MULTICULTURAL, FormStatus.ENTERED),
                form(1001L, FormType.REGULAR, FormStatus.PASSED)
        ));

        try (Workbook workbook = WorkbookFactory.create(exportFinalPassedFormUseCase.execute().getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(FIRST_ROW + 4);
            long[] expected = {1001L, 1002L, 2001L, 2002L, 3001L};
            for (int i = 0; i < expected.length; i++) {
                assertThat(sheet.getRow(FIRST_ROW + i).getCell(FINAL_EXAMINATION_NUMBER).getNumericCellValue())
                        .isEqualTo(expected[i]);
            }
            assertThat(sheet.getRow(FIRST_ROW).getCell(FINAL_TYPE).getStringCellValue())
                    .isEqualTo(FormType.REGULAR.getDescription());
            assertThat(sheet.getRow(FIRST_ROW + 4).getCell(FINAL_TYPE).getStringCellValue())
                    .isEqualTo(FormType.NATIONAL_VETERANS_EDUCATION.getDescription());
        }
    }

    private Form form(Long examinationNumber, FormType type, FormStatus status) {
        Form form = FormFixture.createForm(type);
        ReflectionTestUtils.setField(form, "id", examinationNumber);
        ReflectionTestUtils.setField(form, "examinationNumber", examinationNumber);
        ReflectionTestUtils.setField(form, "status", status);
        // 엑셀의 졸업 구분 컬럼은 yyyy-MM-dd 형식의 졸업일을 요구한다.
        ReflectionTestUtils.setField(form.getEducation(), "graduationDate", "2021-02-15");
        form.getScore().updateSecondRoundScore(50.0, 20.0);
        return form;
    }
}
