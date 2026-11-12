package com.bamdoliro.maru.infrastructure.scheduler;

import com.bamdoliro.maru.application.schedule.ExecuteFirstPassSelectionUseCase;
import com.bamdoliro.maru.application.schedule.event.AdmissionScheduleChangedEvent;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.exception.FirstPassAlreadyExecutedException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleNotFoundException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.shared.config.TimeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

/**
 * 활성 입학 일정의 selectFirstPassAt 시각에 1차 합격자 선발을 예약한다.
 * - 서버가 뜨면(ApplicationReadyEvent) 활성 일정 기준으로 예약한다.
 * - 일정이 등록/수정되면(AdmissionScheduleChangedEvent, 커밋 후) 기존 예약을 취소하고 다시 예약한다.
 * 여러 서버가 같은 시각에 실행해도 FirstPassSelectionExecution의 unique 제약으로 한 번만 실행된다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AdmissionScheduleTaskScheduler {

    private final TaskScheduler admissionTaskScheduler;
    private final AdmissionScheduleFacade admissionScheduleFacade;
    private final ExecuteFirstPassSelectionUseCase executeFirstPassSelectionUseCase;
    private final Clock serviceClock;
    private ScheduledFuture<?> scheduledTask;

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void scheduleOnStartup() {
        try {
            schedule(admissionScheduleFacade.getCurrentSchedule());
        } catch (ScheduleNotFoundException e) {
            log.warn("First-pass selection is not scheduled until an admission schedule is configured.");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public synchronized void reschedule(AdmissionScheduleChangedEvent event) {
        schedule(admissionScheduleFacade.getSchedule(event.scheduleId()));
    }

    private void schedule(AdmissionSchedule schedule) {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }

        LocalDateTime executionTime = schedule.getSelectFirstPassAt();
        if (Objects.isNull(executionTime)) {
            log.warn("First-pass selection time is not set for admission year {}", schedule.getAdmissionYear());
            return;
        }
        if (!executionTime.isAfter(LocalDateTime.now(serviceClock))) {
            log.info("Skip past first-pass selection schedule for admission year {} ({})",
                    schedule.getAdmissionYear(), executionTime);
            return;
        }

        Long scheduleId = schedule.getId();
        scheduledTask = admissionTaskScheduler.schedule(
                () -> execute(scheduleId),
                executionTime.atZone(TimeConfig.SERVICE_ZONE_ID).toInstant()
        );
        log.info("Scheduled first-pass selection for admission year {} at {}",
                schedule.getAdmissionYear(), executionTime);
    }

    private void execute(Long scheduleId) {
        try {
            executeFirstPassSelectionUseCase.execute(scheduleId);
            log.info("Executed first-pass selection: {}", scheduleId);
        } catch (FirstPassAlreadyExecutedException | DataIntegrityViolationException e) {
            log.info("First-pass selection was already executed or claimed by another server: {}", scheduleId);
        } catch (Exception e) {
            log.error("Failed to execute first-pass selection: {}", scheduleId, e);
        }
    }
}
