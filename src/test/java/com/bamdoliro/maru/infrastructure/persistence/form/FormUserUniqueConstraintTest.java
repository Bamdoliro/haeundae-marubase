package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.application.form.SubmitFormUseCase;
import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.FormAlreadySubmittedException;
import com.bamdoliro.maru.domain.form.domain.value.Score;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.presentation.form.dto.request.SubmitFormRequest;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.constants.Schedule;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;

/**
 * tbl_form.user_id 유니크 제약을 애플리케이션 검사 없이 DB 수준에서 직접 확인한다.
 */
@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class FormUserUniqueConstraintTest {

    @SpyBean
    private FormRepository formRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubmitFormUseCase submitFormUseCase;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void 같은_사용자로_원서를_두_개_저장하면_제약_위반이_발생한다() {
        User user = userRepository.save(UserFixture.createUser());
        formRepository.saveAndFlush(scoredForm(user));

        Form duplicated = scoredForm(user);
        ReflectionTestUtils.setField(duplicated.getApplicant(), "registrationNumber", "070605-3111110");

        assertThatThrownBy(() -> formRepository.saveAndFlush(duplicated))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("user_id");
        assertThat(formRepository.count()).isEqualTo(1);
    }

    @Test
    void 다른_사용자의_원서는_각각_저장된다() {
        formRepository.saveAndFlush(scoredForm(userRepository.save(UserFixture.createUser())));
        formRepository.saveAndFlush(scoredForm(userRepository.save(UserFixture.createUser())));

        assertThat(formRepository.count()).isEqualTo(2);
    }

    @Test
    void 중복_검사를_통과해도_DB_제약에_걸리면_이미_제출한_원서_에러로_변환된다() {
        User user = userRepository.save(UserFixture.createUser());
        submit(user, requestWithRegistrationNumber("070605-3111118"));

        // 애플리케이션의 중복 검사가 뚫린 상황을 만든다.
        doReturn(Optional.empty()).when(formRepository).findByUser(any());

        assertThatThrownBy(() -> submit(user, requestWithRegistrationNumber("070605-3111119")))
                .isInstanceOf(FormAlreadySubmittedException.class);
        assertThat(formRepository.count()).isEqualTo(1);
    }

    private Form scoredForm(User user) {
        Form form = FormFixture.createRandomBusanForm(user);
        form.updateScore(new Score(100.0, 0));
        return form;
    }

    private void submit(User user, SubmitFormRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try (MockedStatic<Schedule> schedule = mockStatic(Schedule.class, CALLS_REAL_METHODS)) {
            schedule.when(Schedule::now).thenReturn(Schedule.START.plusDays(1));
            submitFormUseCase.execute(user, request);
        }
    }

    private SubmitFormRequest requestWithRegistrationNumber(String registrationNumber) {
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        ReflectionTestUtils.setField(request.getApplicant(), "registrationNumber", registrationNumber);
        return request;
    }
}
