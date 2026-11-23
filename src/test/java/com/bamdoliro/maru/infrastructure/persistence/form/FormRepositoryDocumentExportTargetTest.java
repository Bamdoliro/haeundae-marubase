package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.value.Score;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@Transactional
@SpringBootTest
class FormRepositoryDocumentExportTargetTest {

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 최종_제출_전이거나_반려된_원서는_빼고_id_순으로_조회한다() {
        Long approved = save(Form::approve);
        save(form -> {});
        Long finalSubmitted = save(Form::submit);
        save(Form::reject);
        Long firstPassed = save(Form::firstPass);
        Long notArrived = save(Form::notArrive);
        entityManager.flush();
        entityManager.clear();

        List<Long> actual = formRepository.findDocumentExportTargets().stream()
                .map(Form::getId)
                .toList();

        assertThat(actual).containsExactly(approved, finalSubmitted, firstPassed, notArrived);
    }

    private Long save(Consumer<Form> changeStatus) {
        User user = userRepository.save(UserFixture.createUser());
        Form form = FormFixture.createRandomBusanForm(user);
        form.updateScore(new Score(100.0, 0));
        changeStatus.accept(form);
        return formRepository.save(form).getId();
    }
}
