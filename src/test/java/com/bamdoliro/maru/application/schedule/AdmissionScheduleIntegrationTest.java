package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.exception.InvalidSchedulePeriodException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleAlreadyExistsException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleNotFoundException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleVersionConflictException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.persistence.schedule.AdmissionScheduleRepository;
import com.bamdoliro.maru.presentation.schedule.dto.response.ScheduleResponse;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 DB(H2)로 일정 등록/조회/수정과 낙관적 락 동작을 확인한다.
 */
@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class AdmissionScheduleIntegrationTest {

    private static final LocalDateTime CHANGED_REGISTRATION_END = ScheduleFixture.REGISTRATION_END.plusDays(1);
    private static final LocalDateTime OTHER_REGISTRATION_END = ScheduleFixture.REGISTRATION_END.plusDays(2);

    @Autowired
    private CreateScheduleUseCase createScheduleUseCase;

    @Autowired
    private UpdateScheduleUseCase updateScheduleUseCase;

    @Autowired
    private QueryCurrentScheduleUseCase queryCurrentScheduleUseCase;

    @Autowired
    private AdmissionScheduleFacade admissionScheduleFacade;

    @Autowired
    private AdmissionScheduleRepository admissionScheduleRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 등록된_일정이_없으면_현재_일정_조회는_404_예외를_낸다() {
        assertThrows(ScheduleNotFoundException.class, () -> queryCurrentScheduleUseCase.execute());
    }

    @Test
    void 일정을_등록하면_현재_일정으로_조회된다() {
        createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null));

        ScheduleResponse response = queryCurrentScheduleUseCase.execute();
        assertEquals(2027, response.getAdmissionYear());
        assertTrue(response.isActive());
        assertEquals(0L, response.getVersion());
        assertEquals(ScheduleFixture.VISIT_SUBMISSION_AT, response.getVisitSubmissionAt());
        assertEquals(ScheduleFixture.ATTENDANCE_STANDARD_DATE, response.getAttendanceStandardDate());
    }

    @Test
    void 새_학년도_일정을_등록하면_이전_일정은_비활성이_된다() {
        createScheduleUseCase.execute(ScheduleFixture.createRequest(2026, null));
        createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null));

        assertEquals(2027, queryCurrentScheduleUseCase.execute().getAdmissionYear());
        assertFalse(admissionScheduleRepository.findByAdmissionYear(2026).orElseThrow().isActive());
    }

    @Test
    void 같은_학년도는_두_번_등록할_수_없다() {
        createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null));

        assertThrows(ScheduleAlreadyExistsException.class,
                () -> createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null)));
    }

    @Test
    void 시간_순서가_잘못된_일정은_등록되지_않는다() {
        assertThrows(InvalidSchedulePeriodException.class,
                () -> createScheduleUseCase.execute(ScheduleFixture.createInvalidOrderRequest(2027)));
        assertEquals(0, admissionScheduleRepository.count());
    }

    @Test
    void 조회한_version으로_수정하면_반영되고_version이_올라간다() {
        createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null));

        updateScheduleUseCase.execute(ScheduleFixture.createRequest(2027, 0L, CHANGED_REGISTRATION_END));

        ScheduleResponse response = queryCurrentScheduleUseCase.execute();
        assertEquals(1L, response.getVersion());
        assertEquals(CHANGED_REGISTRATION_END, response.getRegistrationEnd());
    }

    @Test
    void 이미_수정된_일정을_이전_version으로_수정하면_충돌한다() {
        createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null));
        updateScheduleUseCase.execute(ScheduleFixture.createRequest(2027, 0L, CHANGED_REGISTRATION_END));

        assertThrows(ScheduleVersionConflictException.class,
                () -> updateScheduleUseCase.execute(ScheduleFixture.createRequest(2027, 0L, OTHER_REGISTRATION_END)));
    }

    @Test
    void 두_관리자가_같은_version으로_동시에_수정하면_한_명만_성공한다() {
        createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null));

        TransactionTemplate firstAdminTransaction = new TransactionTemplate(transactionManager);

        // 첫 번째 관리자가 version 0 일정을 읽은 상태에서, 두 번째 관리자의 수정이 먼저 커밋된다.
        // 첫 번째 관리자는 version 검사는 통과하지만 저장 시 @Version 충돌로 실패해야 한다.
        assertThrows(ScheduleVersionConflictException.class, () -> firstAdminTransaction.executeWithoutResult(status -> {
            AdmissionSchedule loaded = admissionScheduleFacade.getCurrentSchedule();
            assertEquals(0L, loaded.getVersion());

            CompletableFuture.runAsync(
                    () -> updateScheduleUseCase.execute(ScheduleFixture.createRequest(2027, 0L, CHANGED_REGISTRATION_END))
            ).join();

            updateScheduleUseCase.execute(ScheduleFixture.createRequest(2027, 0L, OTHER_REGISTRATION_END));
        }));

        // 먼저 커밋한 두 번째 관리자의 수정만 남는다.
        AdmissionSchedule saved = admissionScheduleRepository.findByAdmissionYear(2027).orElseThrow();
        assertEquals(1L, saved.getVersion());
        assertEquals(CHANGED_REGISTRATION_END, saved.getRegistrationEnd());
    }

    @Test
    void 수정할_때_시간_순서가_잘못되면_반영되지_않는다() {
        createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null));
        LocalDateTime before = queryCurrentScheduleUseCase.execute().getApplicationFormEnd();

        assertThrows(InvalidSchedulePeriodException.class,
                () -> updateScheduleUseCase.execute(ScheduleFixture.createInvalidOrderRequest(2027, 0L)));

        assertEquals(before, queryCurrentScheduleUseCase.execute().getApplicationFormEnd());
    }
}
