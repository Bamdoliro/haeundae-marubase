package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.service.AssignExaminationNumberService;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.constants.FixedNumber;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class ExportFirstRoundResultUseCaseTest {

    @Autowired
    private ExportFirstRoundResultUseCase exportFirstRoundResultUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignExaminationNumberService assignExaminationNumberService;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private CalculateFormScoreService calculateFormScoreService;

    @Autowired
    private SelectFirstPassUseCase selectFirstPassUseCase;

    @BeforeEach
    void setUp() {
        List<User> userList = userRepository.saveAll(
                UserFixture.generateUserList(FixedNumber.TOTAL * 2)
        );
        List<Form> formList = FormFixture.generateBusanFormList(userList);
        formList.forEach(form -> {
            assignExaminationNumberService.execute(form);
            form.assignInterviewNumber(String.valueOf(form.getExaminationNumber()));
            form.approve();
            calculateFormScoreService.execute(form);
            formRepository.save(form);
        });

        selectFirstPassUseCase.execute();
    }

    @Test
    void 정상적으로_1차전형_결과를_엑셀로_저장한다() throws IOException {
        Resource resource = exportFirstRoundResultUseCase.execute();
        SaveFileUtil.execute(resource, SaveFileUtil.XLSX);

        List<Form> firstRoundFormList = formRepository.findFirstRoundForm();
        long firstPassedCount = firstRoundFormList.stream()
                .filter(form -> Boolean.TRUE.equals(form.isFirstPassed()))
                .count();

        try (InputStream inputStream = resource.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            // 템플릿 시트와 헤더가 그대로 유지된다
            assertEquals("1차 합격자 인적사항(연락처)", workbook.getSheetAt(0).getSheetName());
            assertEquals("일반전형 1단계 성적", workbook.getSheetAt(1).getSheetName());
            assertEquals("기회균등전형 1단계 성적", workbook.getSheetAt(2).getSheetName());
            assertEquals("사회통합전형 1단계 성적", workbook.getSheetAt(3).getSheetName());
            assertEquals("2027학년도 1차 합격자 인적사항(연락처)", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());

            // 시트1에는 1차 합격자만 들어간다
            Sheet contactSheet = workbook.getSheetAt(0);
            assertEquals(firstPassedCount, contactSheet.getLastRowNum() - 1);

            Set<String> passOrFailValues = new HashSet<>();
            int gradeRowCount = 0;
            for (int i = 1; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                assertEquals(24, sheet.getNumMergedRegions());
                assertEquals("접수번호", sheet.getRow(1).getCell(0).getStringCellValue());
                assertEquals("1단계성적", sheet.getRow(1).getCell(38).getStringCellValue());

                for (int r = 5; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    gradeRowCount++;
                    passOrFailValues.add(row.getCell(10).getStringCellValue());
                    double rank = row.getCell(11).getNumericCellValue();
                    assertTrue(rank >= 1 && rank <= sheet.getLastRowNum() - 4);
                }
            }

            // 시트2~4에는 합격자와 불합격자가 모두 들어가고, 합불이 실제 결과대로 표시된다
            assertEquals(firstRoundFormList.size(), gradeRowCount);
            assertEquals(Set.of("합격", "불합격"), passOrFailValues);
        }
    }

    @Test
    void 정원_외_전형_지원자가_있으면_정원_외_전형_시트를_추가한다() throws IOException {
        List<User> userList = userRepository.saveAll(UserFixture.generateUserList(2));
        List<Form> supernumeraryFormList = FormFixture.generateBusanFormList(userList);
        for (int i = 0; i < supernumeraryFormList.size(); i++) {
            Form form = supernumeraryFormList.get(i);
            form.update(form.getApplicant(), form.getParent(), form.getEducation(), form.getGrade(), form.getDocument(), FormType.SPECIAL_ADMISSION);
            assignExaminationNumberService.execute(form);
            calculateFormScoreService.execute(form);
            if (i == 0) {
                form.firstPass();
            } else {
                form.firstFail();
            }
            formRepository.save(form);
        }

        Resource resource = exportFirstRoundResultUseCase.execute();
        SaveFileUtil.execute(resource, SaveFileUtil.XLSX);

        try (InputStream inputStream = resource.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals(5, workbook.getNumberOfSheets());
            Sheet sheet = workbook.getSheetAt(4);
            assertEquals("정원 외 전형 1단계 성적", sheet.getSheetName());
            assertEquals("정원 외 전형 지원자 성적", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(24, sheet.getNumMergedRegions());
            assertEquals("접수번호", sheet.getRow(1).getCell(0).getStringCellValue());

            Set<String> passOrFailValues = new HashSet<>();
            for (int r = 5; r <= sheet.getLastRowNum(); r++) {
                assertEquals("특례입학대상자", sheet.getRow(r).getCell(9).getStringCellValue());
                passOrFailValues.add(sheet.getRow(r).getCell(10).getStringCellValue());
            }
            assertEquals(Set.of("합격", "불합격"), passOrFailValues);

            // 복제 원본인 일반전형 시트에는 정원 외 전형 지원자가 섞이지 않는다
            Sheet regularSheet = workbook.getSheetAt(1);
            for (int r = 5; r <= regularSheet.getLastRowNum(); r++) {
                assertEquals("일반전형", regularSheet.getRow(r).getCell(9).getStringCellValue());
            }
        }
    }
}
