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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
class ExportFirstScoreUseCaseTest {

    @Autowired
    private ExportFirstScoreUseCase exportFirstScoreUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private AssignExaminationNumberService assignExaminationNumberService;

    @Autowired
    private CalculateFormScoreService calculateFormScoreService;

    @Test
    void 전체_원서의_1차_점수를_엑셀로_내보내고_학교가_없는_원서는_빈_칸으로_둔다() throws Exception {
        List<User> userList = userRepository.saveAll(UserFixture.generateUserList(3));

        List<Form> scoredForms = FormFixture.generateBusanFormList(userList.subList(0, 2));
        scoredForms.forEach(form -> {
            assignExaminationNumberService.execute(form);
            calculateFormScoreService.execute(form);
            formRepository.save(form);
        });

        Form qualificationForm = FormFixture.createRandomQualificationExaminationForm(userList.get(2));
        assignExaminationNumberService.execute(qualificationForm);
        calculateFormScoreService.execute(qualificationForm);
        formRepository.save(qualificationForm);

        Resource resource = exportFirstScoreUseCase.execute();

        try (InputStream inputStream = resource.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("총점", sheet.getRow(0).getCell(13).getStringCellValue());
            assertEquals(3, sheet.getLastRowNum());

            Row qualificationRow = findRow(sheet, qualificationForm.getId());
            assertTrue(isBlank(qualificationRow.getCell(10)), "검정고시 원서는 학교명이 비어 있어야 한다");
            assertTrue(qualificationRow.getCell(12).getNumericCellValue() > 0);

            Row scoredRow = findRow(sheet, scoredForms.get(0).getId());
            assertEquals("부산광역시", scoredRow.getCell(8).getStringCellValue());
            assertEquals(scoredForms.get(0).getScore().getFirstRoundScore(), scoredRow.getCell(13).getNumericCellValue(), 0.001);
        }
    }

    private static Row findRow(Sheet sheet, Long formId) {
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if ((long) row.getCell(0).getNumericCellValue() == formId) {
                return row;
            }
        }
        throw new AssertionError("원서 " + formId + " 행이 없습니다.");
    }

    private static boolean isBlank(Cell cell) {
        return cell == null || cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK;
    }
}
