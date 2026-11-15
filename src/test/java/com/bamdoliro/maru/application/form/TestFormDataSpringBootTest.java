package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.log.AdminFormStatusChangeLog;
import com.bamdoliro.maru.domain.log.AdminFromViewLog;
import com.bamdoliro.maru.domain.log.FormUpdateLog;
import com.bamdoliro.maru.domain.log.UpdatedField;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.message.SendMessageService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminFormStatusChangeLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminFormViewLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.FormUpdateLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.UpdatedFieldRepository;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.presentation.form.dto.request.GenerateTestFormRequest;
import com.bamdoliro.maru.presentation.form.dto.request.TestFormCountRequest;
import com.bamdoliro.maru.presentation.form.dto.response.DeleteTestFormResponse;
import com.bamdoliro.maru.presentation.form.dto.response.GenerateTestFormResponse;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class TestFormDataSpringBootTest {

    @Autowired
    private GenerateTestFormUseCase generateTestFormUseCase;

    @Autowired
    private DeleteTestFormUseCase deleteTestFormUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private AdminFormViewLogRepository adminFormViewLogRepository;

    @Autowired
    private AdminFormStatusChangeLogRepository adminFormStatusChangeLogRepository;

    @Autowired
    private FormUpdateLogRepository formUpdateLogRepository;

    @Autowired
    private UpdatedFieldRepository updatedFieldRepository;

    @MockBean
    private SendMessageService sendMessageService;

    @Test
    void 테스트_원서를_만들면_DB에_테스트_유저와_최종제출_원서가_저장되고_문자는_보내지_않는다() {
        GenerateTestFormResponse response = generateTestFormUseCase.execute(new GenerateTestFormRequest(List.of(
                new TestFormCountRequest(FormType.REGULAR, 5),
                new TestFormCountRequest(FormType.MULTICULTURAL, 3)
        )));

        assertEquals(8, response.getTotalCount());
        List<Form> forms = formRepository.findAllByUser_IsTestDataTrue();
        assertEquals(8, forms.size());
        assertEquals(5, forms.stream().filter(form -> form.getType() == FormType.REGULAR).count());
        assertEquals(3, forms.stream().filter(form -> form.getType() == FormType.MULTICULTURAL).count());
        forms.forEach(form -> {
            assertEquals(FormStatus.FINAL_SUBMITTED, form.getStatus());
            assertTrue(form.getPayment());
            assertNull(form.getExaminationNumber());
        });
        assertEquals(8, userRepository.findAll().stream().filter(User::isTestData).count());
        verifyNoInteractions(sendMessageService);
    }

    @Test
    void 테스트_데이터만_지우고_실제_지원자와_그_로그는_남긴다() {
        // given: 실제 지원자 1명 + 관리자 1명
        User admin = userRepository.save(UserFixture.createAdminUser());
        User realUser = userRepository.save(UserFixture.createUser());
        Form realForm = FormFixture.createForm(FormType.REGULAR);
        ReflectionTestUtils.setField(realForm, "user", realUser);
        formRepository.save(realForm);
        adminFormViewLogRepository.save(new AdminFromViewLog("127.0.0.1", "agent", admin, realForm));

        // given: 테스트 원서 4개 + 관리자가 테스트 원서를 조회/상태변경/수정한 로그
        generateTestFormUseCase.execute(new GenerateTestFormRequest(List.of(
                new TestFormCountRequest(FormType.REGULAR, 2),
                new TestFormCountRequest(FormType.NATIONAL_BASIC_LIVING, 2)
        )));
        Form testForm = formRepository.findAllByUser_IsTestDataTrue().get(0);
        adminFormViewLogRepository.save(new AdminFromViewLog("127.0.0.1", "agent", admin, testForm));
        adminFormStatusChangeLogRepository.save(new AdminFormStatusChangeLog("127.0.0.1", "agent", admin, testForm, FormStatus.FINAL_SUBMITTED, FormStatus.RECEIVED));
        User testUser = userRepository.findById(testForm.getUser().getId()).orElseThrow();
        FormUpdateLog updateLog = formUpdateLogRepository.save(new FormUpdateLog(null, testUser.getPhoneNumber(), "127.0.0.1", "agent", testUser, testForm));
        updatedFieldRepository.save(new UpdatedField("name", "a", "b", updateLog));

        // when
        DeleteTestFormResponse response = deleteTestFormUseCase.execute();

        // then
        assertEquals(4, response.getDeletedFormCount());
        assertEquals(4, response.getDeletedUserCount());
        assertEquals(4, response.getDeletedLogCount());

        assertTrue(formRepository.findAllByUser_IsTestDataTrue().isEmpty());
        assertFalse(userRepository.findAll().stream().anyMatch(User::isTestData));
        assertEquals(1, formRepository.count());
        assertEquals(2, userRepository.count());
        assertEquals(1, adminFormViewLogRepository.count());
        assertEquals(0, adminFormStatusChangeLogRepository.count());
        assertEquals(0, formUpdateLogRepository.count());
        assertEquals(0, updatedFieldRepository.count());
    }

    @Test
    void 테스트_데이터가_없으면_아무것도_지우지_않는다() {
        User realUser = userRepository.save(UserFixture.createUser());

        DeleteTestFormResponse response = deleteTestFormUseCase.execute();

        assertEquals(0, response.getDeletedFormCount());
        assertEquals(0, response.getDeletedUserCount());
        assertEquals(0, response.getDeletedLogCount());
        assertTrue(userRepository.existsById(realUser.getId()));
    }
}
