package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.domain.form.exception.InvalidFormResultRequestException;
import com.bamdoliro.maru.domain.form.exception.InvalidFormStatusException;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.presentation.form.dto.request.PassOrFailFormListRequest;
import com.bamdoliro.maru.presentation.form.dto.request.PassOrFailFormRequest;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class PassOrFailFormUseCaseTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalculateFormScoreService calculateFormScoreService;

    @Autowired
    private PassOrFailFormUseCase passOrFailFormUseCase;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void 정상적으로_합격자를_처리한다() {
        // given
        Long firstPassed = save(Form::firstPass);
        Long passed = save(Form::pass);
        Long failed = save(Form::fail);

        PassOrFailFormListRequest request = request(
                new PassOrFailFormRequest(failed, true),
                new PassOrFailFormRequest(firstPassed, false),
                new PassOrFailFormRequest(passed, false)
        );

        // when
        passOrFailFormUseCase.execute(request);

        // then
        assertThat(statusOf(firstPassed)).isEqualTo(FormStatus.FAILED);
        assertThat(statusOf(passed)).isEqualTo(FormStatus.FAILED);
        assertThat(statusOf(failed)).isEqualTo(FormStatus.PASSED);
    }

    @Test
    void 존재하지_않는_원서를_처리하려고_하면_에러가_발생한다() {
        // given
        Long firstPassed = save(Form::firstPass);

        PassOrFailFormListRequest request = request(
                new PassOrFailFormRequest(firstPassed, true),
                new PassOrFailFormRequest(firstPassed + 100, false)
        );

        // when and then
        assertThatThrownBy(() -> passOrFailFormUseCase.execute(request))
                .isInstanceOf(FormNotFoundException.class);
        assertThat(statusOf(firstPassed)).isEqualTo(FormStatus.FIRST_PASSED);
    }

    @Test
    void 처리할_수_없는_상태의_원서가_섞여_있으면_에러가_발생하고_아무것도_바뀌지_않는다() {
        // given
        Long firstPassed = save(Form::firstPass);
        Long passed = save(Form::pass);
        Long submitted = save(form -> {
        });
        Long approved = save(Form::approve);

        PassOrFailFormListRequest request = request(
                new PassOrFailFormRequest(firstPassed, true),
                new PassOrFailFormRequest(passed, false),
                new PassOrFailFormRequest(submitted, true),
                new PassOrFailFormRequest(approved, true)
        );

        // when and then
        assertThatThrownBy(() -> passOrFailFormUseCase.execute(request))
                .isInstanceOf(InvalidFormStatusException.class);
        assertThat(statusOf(firstPassed)).isEqualTo(FormStatus.FIRST_PASSED);
        assertThat(statusOf(passed)).isEqualTo(FormStatus.PASSED);
        assertThat(statusOf(submitted)).isEqualTo(FormStatus.SUBMITTED);
        assertThat(statusOf(approved)).isEqualTo(FormStatus.APPROVED);
    }

    @Test
    void 허용되지_않는_상태는_모두_거부한다() {
        for (FormStatus status : FormStatus.values()) {
            if (status == FormStatus.FIRST_PASSED || status == FormStatus.PASSED || status == FormStatus.FAILED) {
                continue;
            }
            Long formId = save(form -> setStatus(form, status));

            assertThatThrownBy(() -> passOrFailFormUseCase.execute(request(new PassOrFailFormRequest(formId, true))))
                    .as(status.name())
                    .isInstanceOf(InvalidFormStatusException.class);
            assertThat(statusOf(formId)).isEqualTo(status);
        }
    }

    @Test
    void 같은_원서가_중복으로_요청되면_에러가_발생하고_아무것도_바뀌지_않는다() {
        // given
        Long firstPassed = save(Form::firstPass);
        Long other = save(Form::firstPass);

        PassOrFailFormListRequest request = request(
                new PassOrFailFormRequest(firstPassed, true),
                new PassOrFailFormRequest(other, true),
                new PassOrFailFormRequest(firstPassed, false)
        );

        // when and then
        assertThatThrownBy(() -> passOrFailFormUseCase.execute(request))
                .isInstanceOf(InvalidFormResultRequestException.class)
                .hasMessageContaining("같은 원서가 두 번 이상");
        assertThat(statusOf(firstPassed)).isEqualTo(FormStatus.FIRST_PASSED);
        assertThat(statusOf(other)).isEqualTo(FormStatus.FIRST_PASSED);
    }

    @Test
    void 원서_번호가_비어_있으면_에러가_발생한다() {
        // given
        Long firstPassed = save(Form::firstPass);

        PassOrFailFormListRequest request = request(
                new PassOrFailFormRequest(firstPassed, true),
                new PassOrFailFormRequest(null, false)
        );

        // when and then
        assertThatThrownBy(() -> passOrFailFormUseCase.execute(request))
                .isInstanceOf(InvalidFormResultRequestException.class)
                .hasMessageContaining("원서 번호가 비어 있는");
        assertThat(statusOf(firstPassed)).isEqualTo(FormStatus.FIRST_PASSED);
    }

    @Test
    void 다른_트랜잭션이_원서를_잠그고_있으면_끝날_때까지_기다린_뒤_바뀐_상태로_검증한다() throws Exception {
        // given
        Long formId = save(Form::firstPass);
        CountDownLatch locked = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 다른 관리자의 처리: 원서를 잠근 채 잠시 머물다가, 처리할 수 없는 상태로 바꾸고 커밋한다.
        Future<?> holder = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            Form form = formRepository.findByFormIdListForUpdate(List.of(formId)).get(0);
            locked.countDown();
            sleep(500);
            form.approve();
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        // when
        long startedAt = System.nanoTime();
        Future<?> second = executor.submit(() ->
                passOrFailFormUseCase.execute(request(new PassOrFailFormRequest(formId, true))));

        // then
        holder.get(10, TimeUnit.SECONDS);
        Throwable failure = null;
        try {
            second.get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            failure = e.getCause();
        }
        long waitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        executor.shutdown();

        // 락이 없으면 잠금 전 상태(FIRST_PASSED)를 읽고 PASSED로 덮어쓴다.
        assertThat(failure).isInstanceOf(InvalidFormStatusException.class);
        assertThat(statusOf(formId)).isEqualTo(FormStatus.APPROVED);
        assertThat(waitedMillis).isGreaterThanOrEqualTo(300);
    }

    private Long save(Consumer<Form> statusChanger) {
        User user = userRepository.save(UserFixture.createUser());
        Form form = FormFixture.createRandomBusanForm(user);
        calculateFormScoreService.execute(form);
        statusChanger.accept(form);
        return formRepository.save(form).getId();
    }

    private void setStatus(Form form, FormStatus status) {
        org.springframework.test.util.ReflectionTestUtils.setField(form, "status", status);
    }

    private FormStatus statusOf(Long formId) {
        return formRepository.findById(formId).orElseThrow().getStatus();
    }

    private PassOrFailFormListRequest request(PassOrFailFormRequest... requests) {
        return new PassOrFailFormListRequest(new ArrayList<>(Arrays.asList(requests)));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
