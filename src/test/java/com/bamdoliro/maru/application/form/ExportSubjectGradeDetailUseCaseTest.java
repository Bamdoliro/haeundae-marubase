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
import org.apache.poi.ss.usermodel.CellType;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class ExportSubjectGradeDetailUseCaseTest {

    @Autowired
    private ExportSubjectGradeDetailUseCase exportSubjectGradeDetailUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private AssignExaminationNumberService assignExaminationNumberService;

    @Autowired
    private CalculateFormScoreService calculateFormScoreService;

    private Form save(Form form, boolean passed) {
        assignExaminationNumberService.execute(form);
        calculateFormScoreService.execute(form);
        if (passed) {
            form.pass();
        }
        return formRepository.save(form);
    }

    @Test
    void 과목_구성이_다른_합격자들도_빈_칸으로_채워_내보낸다() throws Exception {
        List<User> userList = userRepository.saveAll(UserFixture.generateUserList(3));
        Form regularForm = save(FormFixture.createRandomBusanForm(userList.get(0)), true);
        Form qualificationForm = save(FormFixture.createRandomQualificationExaminationForm(userList.get(1)), true);
        Form notPassedForm = save(FormFixture.createRandomBusanForm(userList.get(2)), false);

        Resource resource = exportSubjectGradeDetailUseCase.execute();

        try (InputStream inputStream = resource.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = headers(sheet.getRow(0));
            assertEquals(List.of("ID", "수험번호", "이름", "전화번호", "생년월일", "학교명"), headers.subList(0, 6));

            int regularSubjectColumn = headers.indexOf("2학년 1학기 국어");
            int qualificationSubjectColumn = headers.indexOf("검정고시 국어");
            assertTrue(regularSubjectColumn > 0, "일반 교과 과목 열이 있어야 한다: " + headers);
            assertTrue(qualificationSubjectColumn > 0, "검정고시 과목 열이 있어야 한다: " + headers);

            // 최종 합격자 두 명만 나온다.
            assertEquals(2, sheet.getLastRowNum());

            Row regularRow = findRow(sheet, regularForm.getId());
            assertFalse(isBlank(regularRow.getCell(regularSubjectColumn)));
            assertTrue(isBlank(regularRow.getCell(qualificationSubjectColumn)));

            Row qualificationRow = findRow(sheet, qualificationForm.getId());
            assertTrue(isBlank(qualificationRow.getCell(regularSubjectColumn)));
            assertFalse(isBlank(qualificationRow.getCell(qualificationSubjectColumn)));
            assertTrue(isBlank(qualificationRow.getCell(5)), "검정고시 합격자는 학교명이 비어 있어야 한다");

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                assertTrue((long) sheet.getRow(r).getCell(0).getNumericCellValue() != notPassedForm.getId());
            }
        }
    }

    private static List<String> headers(Row headerRow) {
        List<String> headers = new ArrayList<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            headers.add(headerRow.getCell(c).getStringCellValue());
        }
        return headers;
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
        return cell == null || cell.getCellType() == CellType.BLANK;
    }
}
