package com.bamdoliro.maru.infrastructure.scheduler;

import com.bamdoliro.maru.application.schedule.CreateScheduleUseCase;
import com.bamdoliro.maru.application.schedule.ExecuteFirstPassSelectionUseCase;
import com.bamdoliro.maru.application.schedule.UpdateScheduleUseCase;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.domain.FirstPassSelectionExecution;
import com.bamdoliro.maru.domain.schedule.exception.FirstPassAlreadyExecutedException;
import com.bamdoliro.maru.infrastructure.persistence.schedule.AdmissionScheduleRepository;
import com.bamdoliro.maru.infrastructure.persistence.schedule.FirstPassSelectionExecutionRepository;
import com.bamdoliro.maru.presentation.schedule.dto.request.ScheduleRequest;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.config.TimeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 TaskScheduler와 DB(H2)로, 일정 등록/수정 후 selectFirstPassAt 시각에 1차 선발이 실제로 실행되는지 확인한다.
 */
@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class AdmissionScheduleTaskSchedulerIntegrationTest {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(15);

    @Autowired
    private CreateScheduleUseCase createScheduleUseCase;

    @Autowired
    private UpdateScheduleUseCase updateScheduleUseCase;

    @Autowired
    private ExecuteFirstPassSelectionUseCase executeFirstPassSelectionUseCase;

    @Autowired
    private AdmissionScheduleRepository admissionScheduleRepository;

    @Autowired
    private FirstPassSelectionExecutionRepository executionRepository;

    @Test
    void 일정을_등록하면_선발_시각에_1차_선발이_한_번_실행된다() throws InterruptedException {
        LocalDateTime now = now();
        Long scheduleId = createScheduleUseCase.execute(request(now, now.plusSeconds(2), null)).getId();

        awaitExecutionCount(1);

        assertEquals(1, executionRepository.count());
        FirstPassSelectionExecution execution = executionRepository.findAll().get(0);
        assertEquals(scheduleId, execution.getSchedule().getId());
        assertEquals(now.plusSeconds(2), execution.getExecutionTime());
    }

    @Test
    void 일정을_수정하면_바뀐_선발_시각으로_다시_예약된다() throws InterruptedException {
        LocalDateTime now = now();
        createScheduleUseCase.execute(request(now, now.plusHours(1), null));

        updateScheduleUseCase.execute(request(now, now.plusSeconds(2), 0L));

        awaitExecutionCount(1);
        assertEquals(1, executionRepository.count());
    }

    @Test
    void 선발이_실행된_일정을_수정해_새_시각으로_예약하면_그_시각에_다시_실행된다() throws InterruptedException {
        LocalDateTime now = now();
        Long scheduleId = createScheduleUseCase.execute(request(now, now.plusSeconds(2), null)).getId();
        awaitExecutionCount(1);

        LocalDateTime changedTime = now().plusSeconds(2);
        updateScheduleUseCase.execute(request(now, changedTime, 0L));
        awaitExecutionCount(2);

        assertEquals(2, executionRepository.count());
        assertTrue(executionRepository.existsByScheduleAndExecutionTime(
                admissionScheduleRepository.findById(scheduleId).orElseThrow(), changedTime));
    }

    @Test
    void 같은_일정이라도_시각을_바꿔서_다시_실행하면_성공한다() {
        LocalDateTime now = now();
        LocalDateTime firstTime = now.plusHours(1);
        LocalDateTime changedTime = now.plusHours(2);
        Long scheduleId = createScheduleUseCase.execute(request(now, firstTime, null)).getId();
        executeFirstPassSelectionUseCase.execute(scheduleId, firstTime);

        updateScheduleUseCase.execute(request(now, changedTime, 0L));
        executeFirstPassSelectionUseCase.execute(scheduleId, changedTime);

        assertEquals(2, executionRepository.count());
    }

    @Test
    void 같은_일정_같은_시각으로는_중복_실행되지_않는다() {
        LocalDateTime now = now();
        LocalDateTime executionTime = now.plusHours(1);
        Long scheduleId = createScheduleUseCase.execute(request(now, executionTime, null)).getId();

        executeFirstPassSelectionUseCase.execute(scheduleId, executionTime);

        assertThrows(FirstPassAlreadyExecutedException.class,
                () -> executeFirstPassSelectionUseCase.execute(scheduleId, executionTime));
        assertEquals(1, executionRepository.count());
    }

    @Test
    void 다른_서버가_같은_일정_같은_시각의_실행_기록을_먼저_남겼다면_unique_제약으로_막힌다() {
        LocalDateTime now = now();
        LocalDateTime executionTime = now.plusHours(1);
        Long scheduleId = createScheduleUseCase.execute(request(now, executionTime, null)).getId();
        AdmissionSchedule schedule = admissionScheduleRepository.findById(scheduleId).orElseThrow();
        executionRepository.saveAndFlush(new FirstPassSelectionExecution(schedule, executionTime));

        assertThrows(DataIntegrityViolationException.class,
                () -> executionRepository.saveAndFlush(new FirstPassSelectionExecution(schedule, executionTime)));
        assertEquals(1, executionRepository.count());
    }

    @Test
    void 같은_일정이라도_실행_시각이_다르면_실행_기록을_따로_남길_수_있다() {
        LocalDateTime now = now();
        Long scheduleId = createScheduleUseCase.execute(request(now, now.plusHours(1), null)).getId();
        AdmissionSchedule schedule = admissionScheduleRepository.findById(scheduleId).orElseThrow();

        executionRepository.saveAndFlush(new FirstPassSelectionExecution(schedule, now.plusHours(1)));
        executionRepository.saveAndFlush(new FirstPassSelectionExecution(schedule, now.plusHours(2)));

        assertEquals(2, executionRepository.count());
    }

    private void awaitExecutionCount(long expected) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
        while (executionRepository.count() < expected && System.nanoTime() < deadline) {
            Thread.sleep(200);
        }
        assertTrue(executionRepository.count() >= expected, "1차 선발이 예약 시각에 실행되지 않았습니다.");
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(TimeConfig.SERVICE_ZONE_ID).truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * 원서 접수가 이미 끝났고, selectFirstPassAt 이후로 나머지 일정이 이어지는 요청.
     */
    private static ScheduleRequest request(LocalDateTime now, LocalDateTime selectFirstPassAt, Long version) {
        LocalDateTime applicationFormStart = now.minusDays(10);
        LocalDateTime announcementOfFirstPass = selectFirstPassAt.plusDays(1);
        return new ScheduleRequest(
                2027,
                version,
                applicationFormStart,
                applicationFormStart.plusDays(1),
                now.minusHours(1),
                selectFirstPassAt,
                announcementOfFirstPass,
                announcementOfFirstPass.plusDays(1),
                announcementOfFirstPass.plusDays(1).plusHours(1),
                announcementOfFirstPass.plusDays(2),
                announcementOfFirstPass.plusDays(3),
                announcementOfFirstPass.plusDays(4),
                announcementOfFirstPass.plusDays(3),
                announcementOfFirstPass.plusDays(4),
                applicationFormStart.toLocalDate(),
                null,
                null
        );
    }
}
