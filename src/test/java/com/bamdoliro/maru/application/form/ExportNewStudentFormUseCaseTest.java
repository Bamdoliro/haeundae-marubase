package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.service.AssignExaminationNumberService;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import com.bamdoliro.maru.shared.util.SaveFileUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class ExportNewStudentFormUseCaseTest {

    private static final int FORM_COUNT = 60;

    @Autowired
    private ExportNewStudentFormUseCase exportNewStudentFormUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignExaminationNumberService assignExaminationNumberService;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private CalculateFormScoreService calculateFormScoreService;

    @BeforeEach
    void setUp() {
        List<User> userList = userRepository.saveAll(UserFixture.generateUserList(FORM_COUNT));
        List<Form> formList = FormFixture.generateBusanFormList(userList);
        formList.forEach(form -> {
            assignExaminationNumberService.execute(form);
            calculateFormScoreService.execute(form);
            form.enter();
            formRepository.save(form);
        });
    }

    @Test
    void 신입생_명단을_학교_서식에_맞춰_엑셀로_저장한다() throws Exception {
        Resource resource = exportNewStudentFormUseCase.execute();
        SaveFileUtil.execute(resource, SaveFileUtil.XLSX);

        try (InputStream inputStream = resource.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            assertEquals("신입생 인적사항(연락처)", workbook.getSheetAt(0).getSheetName());
            assertEquals("일반전형 1단계 성적", workbook.getSheetAt(1).getSheetName());
            assertEquals("기회균등전형 1단계 성적", workbook.getSheetAt(2).getSheetName());
            assertEquals("사회통합전형 1단계 성적", workbook.getSheetAt(3).getSheetName());

            Sheet contactSheet = workbook.getSheetAt(0);
            assertEquals(FORM_COUNT, contactSheet.getLastRowNum() - 1);

            int gradeRowCount = 0;
            for (int i = 1; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                assertEquals("접수번호", sheet.getRow(1).getCell(0).getStringCellValue());
                assertEquals("중학교졸업(예정)", sheet.getRow(1).getCell(12).getStringCellValue());
                assertEquals("1단계성적", sheet.getRow(1).getCell(38).getStringCellValue());
                assertEquals("국어", sheet.getRow(4).getCell(12).getStringCellValue());
                assertEquals("결과", sheet.getRow(4).getCell(36).getStringCellValue());

                for (int r = 5; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    gradeRowCount++;
                    assertEquals(row.getCell(0).getNumericCellValue(), row.getCell(1).getNumericCellValue());
                    assertEquals("합격", row.getCell(10).getStringCellValue());
                    assertEquals("중학교졸업예정", row.getCell(5).getStringCellValue());
                    assertEquals("동구", row.getCell(7).getStringCellValue());
                    double rank = row.getCell(11).getNumericCellValue();
                    assertTrue(rank >= 1 && rank <= sheet.getLastRowNum() - 4);
                }
            }
            assertEquals(FORM_COUNT, gradeRowCount);
        }
    }
}
