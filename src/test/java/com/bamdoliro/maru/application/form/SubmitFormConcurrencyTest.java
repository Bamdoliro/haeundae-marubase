package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.FormAlreadySubmittedException;
import com.bamdoliro.maru.domain.form.service.ExaminationNumberLock;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.presentation.form.dto.request.SubmitFormRequest;
import com.bamdoliro.maru.infrastructure.persistence.schedule.AdmissionScheduleRepository;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.config.FixedClockConfig;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2에는 pg_advisory_xact_lock이 없으므로, 같은 의미(트랜잭션이 끝날 때 해제, 같은 트랜잭션에서 재진입 가능)의
 * JVM 락을 ExaminationNumberLock으로 끼워 넣고 SubmitFormUseCase의 동시성 동작을 검증한다.
 */
@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@Import(FixedClockConfig.class)
@SpringBootTest
class SubmitFormConcurrencyTest {

    @Autowired
    private SubmitFormUseCase submitFormUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdmissionScheduleRepository admissionScheduleRepository;

    @Autowired
    private FormRepository formRepository;

    @BeforeEach
    void setUp() {
        admissionScheduleRepository.save(ScheduleFixture.createSchedule(2027, true));
    }

    @Test
    void 같은_전형으로_동시에_제출해도_수험번호가_중복되지_않는다() throws Exception {
        User first = userRepository.save(UserFixture.createUser());
        User second = userRepository.save(UserFixture.createUser());
        SubmitFormRequest firstRequest = requestWithRegistrationNumber("070605-3111118");
        SubmitFormRequest secondRequest = requestWithRegistrationNumber("070605-3111119");

        List<Throwable> failures = runConcurrently(List.of(
                () -> submit(first, firstRequest),
                () -> submit(second, secondRequest)
        ));

        assertThat(failures).isEmpty();
        assertThat(formRepository.findAll())
                .extracting(Form::getExaminationNumber)
                .containsExactlyInAnyOrder(111001L, 111002L);
    }

    @Test
    void 같은_사용자가_동시에_두_번_제출하면_하나만_저장되고_나머지는_이미_제출한_원서_에러가_발생한다() throws Exception {
        User user = userRepository.save(UserFixture.createUser());

        List<Throwable> failures = runConcurrently(List.of(
                () -> submit(user, FormFixture.createFormRequest(FormType.REGULAR)),
                () -> submit(user, FormFixture.createFormRequest(FormType.REGULAR))
        ));

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(FormAlreadySubmittedException.class);
        assertThat(formRepository.count()).isEqualTo(1);
    }

    private Void submit(User user, SubmitFormRequest request) {
        // 제출 로그는 현재 HTTP 요청을 쓰므로 작업 스레드마다 직접 준비한다. 원서 접수 기간은 FixedClockConfig 시각과 활성 일정으로 판단한다.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try {
            submitFormUseCase.execute(user, request);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
        return null;
    }

    private List<Throwable> runConcurrently(List<Callable<Void>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        for (Callable<Void> task : tasks) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return task.call();
            }));
        }
        ready.await();
        start.countDown();

        List<Throwable> failures = new ArrayList<>();
        for (Future<Void> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                failures.add(e.getCause());
            }
        }
        executor.shutdown();
        return failures;
    }

    private SubmitFormRequest requestWithRegistrationNumber(String registrationNumber) {
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        ReflectionTestUtils.setField(request.getApplicant(), "registrationNumber", registrationNumber);
        return request;
    }

    @TestConfiguration
    static class TransactionScopedLockConfig {

        @Bean
        @Primary
        ExaminationNumberLock transactionScopedLock() {
            return new TransactionScopedLock();
        }
    }

    static class TransactionScopedLock implements ExaminationNumberLock {

        private final ReentrantLock lock = new ReentrantLock(true);

        @Override
        public void lock() {
            if (!TransactionSynchronizationManager.isActualTransactionActive()) {
                throw new IllegalTransactionStateException("트랜잭션 안에서만 락을 잡을 수 있습니다.");
            }
            if (lock.isHeldByCurrentThread()) {
                return;
            }

            lock.lock();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    lock.unlock();
                }
            });
        }
    }
}
