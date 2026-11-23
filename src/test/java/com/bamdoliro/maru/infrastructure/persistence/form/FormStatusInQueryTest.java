package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.value.Score;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@Transactional
@SpringBootTest
class FormStatusInQueryTest {

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 최종합격자_조회에_합격과_입학만_포함한다() {
        Form passed = saveForm(Form::pass);
        Form entered = saveForm(Form::enter);
        saveForm(Form::submit);
        saveForm(Form::approve);
        saveForm(Form::firstPass);
        saveForm(Form::firstFail);
        saveForm(Form::fail);
        saveForm(Form::noShow);
        saveForm(Form::reject);

        assertThat(formRepository.findByStatusIn(Set.of(FormStatus.PASSED, FormStatus.ENTERED)))
                .containsExactlyInAnyOrder(passed, entered);
    }

    @Test
    void 해당하는_원서가_없으면_빈_목록을_반환한다() {
        saveForm(Form::submit);

        assertThat(formRepository.findByStatusIn(Set.of(FormStatus.PASSED, FormStatus.ENTERED))).isEmpty();
    }

    private Form saveForm(Consumer<Form> changeStatus) {
        User user = userRepository.save(UserFixture.createUser());
        Form form = FormFixture.createRandomBusanForm(user);
        form.updateScore(new Score(100.0, 0));
        changeStatus.accept(form);
        return formRepository.save(form);
    }
}
