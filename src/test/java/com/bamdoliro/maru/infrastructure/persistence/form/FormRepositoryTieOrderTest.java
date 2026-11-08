package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@Transactional
@SpringBootTest
class FormRepositoryTieOrderTest {

    private static final int REPEAT = 5;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 특별전형_승인_원서는_동점이면_id_오름차순으로_조회된다() {
        assertTieOrder(FormType.MULTICULTURAL, formRepository::findApprovedSpecialForm);
    }

    @Test
    void 일반전형_승인_원서는_동점이면_id_오름차순으로_조회된다() {
        assertTieOrder(FormType.REGULAR, formRepository::findApprovedRegularForm);
    }

    @Test
    void 정원외_승인_원서는_동점이면_id_오름차순으로_조회된다() {
        assertTieOrder(FormType.NATIONAL_VETERANS_EDUCATION, formRepository::findApprovedSupernumeraryForm);
    }

    @Test
    void 동점자가_있어도_백분위_점수는_반복_조회해도_같다() {
        // 점수 내림차순: 160, 150, 150, 150, 140 -> 60% 지점(3번째)은 150
        for (double score : new double[]{150, 160, 150, 140, 150}) {
            save(FormType.REGULAR, score);
        }
        flushAndClear();

        for (int i = 0; i < REPEAT; i++) {
            assertThat(formRepository.findFirstRoundScoreAtPercentile(
                    FormType.Category.REGULAR, 0.6, List.of(FormStatus.APPROVED)))
                    .isEqualTo(150.0);
            entityManager.clear();
        }
    }

    private void assertTieOrder(FormType type, Supplier<List<Form>> query) {
        // 동점자 사이에 다른 점수를 끼워 넣어 저장 순서와 점수 순서를 섞는다.
        Long tie1 = save(type, 150);
        Long high = save(type, 160);
        Long tie2 = save(type, 150);
        Long low = save(type, 140);
        Long tie3 = save(type, 150);
        Long tie4 = save(type, 150);
        flushAndClear();

        List<Long> expected = List.of(high, tie1, tie2, tie3, tie4, low);
        for (int i = 0; i < REPEAT; i++) {
            List<Long> actual = new ArrayList<>(query.get().stream().map(Form::getId).toList());
            assertThat(actual).containsExactlyElementsOf(expected);
            entityManager.clear();
        }
    }

    private Long save(FormType type, double firstRoundScore) {
        User user = userRepository.save(UserFixture.createUser());
        Form form = FormFixture.createRandomBusanForm(user);
        form.update(
                form.getApplicant(),
                form.getParent(),
                form.getEducation(),
                form.getGrade(),
                form.getDocument(),
                type
        );
        form.updateScore(new Score(firstRoundScore, 0));
        form.approve();
        return formRepository.save(form).getId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
