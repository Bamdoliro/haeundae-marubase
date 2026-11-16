package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
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
import com.bamdoliro.maru.shared.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
public class SelectSecondPassUseCaseTest {

    @Autowired
    private SelectSecondPassUseCase selectSecondPassUseCase;

    @Autowired
    private SelectFirstPassUseCase selectFirstPassUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private CalculateFormScoreService calculateFormScoreService;

    @Autowired
    private AssignExaminationNumberService assignExaminationNumberService;

    @BeforeEach
    void setUp() {
        List<User> userList = userRepository.saveAll(
                UserFixture.generateUserList(FixedNumber.TOTAL * 2)
        );
        List<Form> formList = FormFixture.generateBusanFormList(userList.subList(0, userList.size() / 2));
        formList.addAll(FormFixture.generateOtherRegionFormList(userList.subList(userList.size() / 2, userList.size())));
        formList.forEach(form -> {
            assignExaminationNumberService.execute(form);
            form.assignInterviewNumber(String.valueOf(form.getExaminationNumber()));
            form.approve();
            calculateFormScoreService.execute(form);
            formRepository.save(form);
        });
        selectFirstPassUseCase.execute();
        List<Form> firstPassedFormList = formRepository.findByStatus(FormStatus.FIRST_PASSED);
        firstPassedFormList.forEach(form -> {
            if (form.getType().isNationalVeteransEducation()) {
                form.getScore().updateSecondRoundMeisterScore(RandomUtil.randomDouble(0, 100), RandomUtil.randomDouble(0, 100));
            } else {
                form.getScore().updateSecondRoundScore(RandomUtil.randomDouble(0, 100), RandomUtil.randomDouble(0, 100));
            }
            formRepository.save(form);
        });
    }

    @Test
    void 정상적으로_2차전형_합격자를_선발한다() {
        selectSecondPassUseCase.execute();

        Comparator<Form> comparator = Comparator
                .comparing(Form::getType)
                .thenComparing(form -> form.getScore().getTotalScore());

        List<Form> formList = formRepository.findAll()
                .stream()
                .filter(form -> form.isPassedNow() || form.isFailedNow())
                .sorted(comparator)
                .toList();

        formList.forEach(form -> {
            log.info("====================");
            log.info("id: {}", form.getId());
            log.info("examinationNumber: {}", form.getExaminationNumber());
            log.info("type: {}", form.getType());
            log.info("score: {}", form.getScore().getTotalScore());
            log.info("status: {}", form.getStatus());
        });
        int passedFormCount = (int) formList.stream().filter(Form::isPassedNow).count();
        assertEquals(FixedNumber.TOTAL, passedFormCount);
    }

    @Test
    void 합격자를_제외한_지원자는_모두_불합격_처리되고_사회통합전형_탈락자는_일반전형으로_전환되지_않는다() {
        Map<Long, FormType> originalTypes = formRepository.findAll().stream()
                .collect(Collectors.toMap(Form::getId, Form::getType));

        selectSecondPassUseCase.execute();

        List<Form> formList = formRepository.findAll();
        List<Form> failedFormList = formList.stream()
                .filter(form -> form.isFailedNow() || form.isFirstFailedNow())
                .toList();
        assertEquals((long) formList.size() - FixedNumber.TOTAL, failedFormList.size());

        // 1차, 2차 모두 사회통합전형 탈락자를 일반전형으로 전환하지 않으므로 모든 원서가 원래 전형을 유지한다.
        assertTrue(formList.stream().noneMatch(Form::getChangedToRegular));
        assertTrue(formList.stream().allMatch(form -> form.getType() == originalTypes.get(form.getId())));

        List<Form> secondFailedSocialIntegrationFormList = formList.stream()
                .filter(Form::isFailedNow)
                .filter(form -> form.getType().isEqualOpportunity() || form.getType().isSocietyDiversity())
                .toList();
        assertFalse(secondFailedSocialIntegrationFormList.isEmpty());
    }
}
