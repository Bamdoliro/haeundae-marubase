package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.List;
import java.util.Random;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class ExportSecondRoundResultUseCaseTest {

    @Autowired
    private ExportSecondRoundResultUseCase exportSecondRoundResultUseCase;

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
        List<User> userList = userRepository.saveAll(
                UserFixture.generateUserList(FixedNumber.TOTAL * 2)
        );
        List<Form> formList = FormFixture.generateBusanFormList(userList);
        formList.forEach(form -> {
            assignExaminationNumberService.execute(form);
            form.assignInterviewNumber(form.getExaminationNumber());

            if (getRandomBoolean()) {
                form.pass();
            } else {
                form.fail();
            }

            calculateFormScoreService.execute(form);
            form.getScore().updateSecondRoundMeisterScore(100.2, 10.4);
            formRepository.save(form);
        });
    }

    private boolean getRandomBoolean() {
        Random random = new Random();
        return random.nextBoolean();
    }

    @Test
    void 정상적으로_2차전형_결과를_다운받는다() throws IOException {
        SaveFileUtil.execute(exportSecondRoundResultUseCase.execute(), SaveFileUtil.XLSX);
    }
}