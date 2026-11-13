package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.value.Document;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxGenerator;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxService;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExportAllPersonalStatementSheetUseCaseTest {

    @Mock
    private FormRepository formRepository;

    private ExportAllPersonalStatementSheetUseCase useCase;

    @BeforeEach
    void setUp() {
        XlsxService xlsxService = new XlsxService();
        useCase = new ExportAllPersonalStatementSheetUseCase(formRepository, new XlsxGenerator(xlsxService));
    }

    @Test
    void 지원자_전체의_수험번호_이름_자기소개서를_findAll_순서대로_엑셀에_담는다() throws IOException {
        // given
        Form regular = formWithDocument(FormType.REGULAR, 1001L, "학습경험", "지원동기", "인성");
        Form equalOpportunity = formWithDocument(FormType.NATIONAL_BASIC_LIVING, 2001L, "가", "나", "다");
        Form societyDiversity = formWithDocument(FormType.MULTICULTURAL, 3001L, "A", "B", "C");
        given(formRepository.findAll()).willReturn(List.of(regular, equalOpportunity, societyDiversity));

        // when
        Resource resource = useCase.execute();

        // then
        try (Workbook workbook = read(resource)) {
            Sheet sheet = workbook.getSheetAt(0);

            assertEquals("수험번호", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("이름", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("자기소개서", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals(3, sheet.getLastRowNum());

            assertRow(sheet.getRow(1), 1001L, regular.getApplicant().getName(), "1. 학습경험\n\n2. 지원동기\n\n3. 인성");
            assertRow(sheet.getRow(2), 2001L, equalOpportunity.getApplicant().getName(), "1. 가\n\n2. 나\n\n3. 다");
            assertRow(sheet.getRow(3), 3001L, societyDiversity.getApplicant().getName(), "1. A\n\n2. B\n\n3. C");

            CellStyle statementStyle = sheet.getRow(1).getCell(2).getCellStyle();
            assertTrue(statementStyle.getWrapText());
            assertFalse(sheet.getRow(1).getCell(0).getCellStyle().getWrapText());
        }
        verify(formRepository, times(1)).findAll();
    }

    @Test
    void 자기소개서가_비어있거나_null인_지원자도_에러_없이_빈_칸으로_나온다() throws IOException {
        // given
        Form noDocument = FormFixture.createForm(FormType.REGULAR);
        ReflectionTestUtils.setField(noDocument, "document", null);
        Form partiallyEmpty = formWithDocument(FormType.REGULAR, 1002L, null, "", null);
        given(formRepository.findAll()).willReturn(List.of(noDocument, partiallyEmpty));

        // when
        Resource resource = useCase.execute();

        // then
        try (Workbook workbook = read(resource)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row noDocumentRow = sheet.getRow(1);
            assertEquals(CellType.BLANK, noDocumentRow.getCell(0).getCellType());
            assertNull(noDocument.getExaminationNumber());
            assertEquals("", noDocumentRow.getCell(2).getStringCellValue());

            assertEquals("1. \n\n2. \n\n3. ", sheet.getRow(2).getCell(2).getStringCellValue());
        }
    }

    @Test
    void 지원자가_없으면_헤더만_있는_엑셀을_내려준다() throws IOException {
        given(formRepository.findAll()).willReturn(List.of());

        try (Workbook workbook = read(useCase.execute())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(0, sheet.getLastRowNum());
            assertEquals("자기소개서", sheet.getRow(0).getCell(2).getStringCellValue());
        }
    }

    private static Form formWithDocument(FormType type, Long examinationNumber, String learningExperience, String statementOfPurpose, String personality) {
        Form form = FormFixture.createForm(type);
        form.assignExaminationNumber(examinationNumber);
        ReflectionTestUtils.setField(form, "document", new Document(learningExperience, statementOfPurpose, personality));
        return form;
    }

    private static void assertRow(Row row, Long examinationNumber, String name, String statement) {
        assertEquals(examinationNumber.doubleValue(), row.getCell(0).getNumericCellValue());
        assertEquals(name, row.getCell(1).getStringCellValue());
        assertEquals(statement, row.getCell(2).getStringCellValue());
    }

    private static Workbook read(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new XSSFWorkbook(inputStream);
        }
    }
}
