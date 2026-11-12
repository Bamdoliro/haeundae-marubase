package com.bamdoliro.maru.infrastructure.scheduler;

import com.bamdoliro.maru.application.schedule.ExecuteFirstPassSelectionUseCase;
import com.bamdoliro.maru.application.schedule.event.AdmissionScheduleChangedEvent;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleNotFoundException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.shared.config.TimeConfig;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdmissionScheduleTaskSchedulerTest {

    // 원서 접수 마감 이후, 1차 선발 전 시점
    private static final LocalDateTime NOW = ScheduleFixture.APPLICATION_FORM_END.plusDays(1);

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    @Mock
    private ExecuteFirstPassSelectionUseCase executeFirstPassSelectionUseCase;

    private AdmissionScheduleTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = schedulerAt(NOW);
    }

    private AdmissionScheduleTaskScheduler schedulerAt(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(TimeConfig.SERVICE_ZONE_ID).toInstant(), TimeConfig.SERVICE_ZONE_ID);
        return new AdmissionScheduleTaskScheduler(taskScheduler, admissionScheduleFacade, executeFirstPassSelectionUseCase, clock);
    }

    private static AdmissionSchedule scheduleWithId(long id) {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(TimeConfig.SERVICE_ZONE_ID).toInstant();
    }

    @Test
    void 서버가_뜨면_활성_일정의_선발_시각으로_예약한다() {
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(scheduleWithId(1L));

        scheduler.scheduleOnStartup();

        verify(taskScheduler).schedule(any(Runnable.class), eq(toInstant(ScheduleFixture.SELECT_FIRST_PASS_AT)));
    }

    @Test
    void 예약된_작업은_해당_일정으로_선발을_실행한다() {
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(scheduleWithId(1L));
        scheduler.scheduleOnStartup();

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(task.capture(), any(Instant.class));
        task.getValue().run();

        verify(executeFirstPassSelectionUseCase).execute(1L);
    }

    @Test
    void 활성_일정이_없으면_예약하지_않는다() {
        given(admissionScheduleFacade.getCurrentSchedule()).willThrow(new ScheduleNotFoundException());

        scheduler.scheduleOnStartup();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void 선발_시각이_이미_지났으면_예약하지_않는다() {
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(scheduleWithId(1L));

        schedulerAt(ScheduleFixture.SELECT_FIRST_PASS_AT).scheduleOnStartup();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void 선발_시각이_비어_있는_기존_일정이면_예약하지_않는다() {
        AdmissionSchedule schedule = scheduleWithId(1L);
        ReflectionTestUtils.setField(schedule, "selectFirstPassAt", null);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);

        scheduler.scheduleOnStartup();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 일정이_바뀌면_기존_예약을_취소하고_새_시각으로_다시_예약한다() {
        ScheduledFuture<Object> previous = mock(ScheduledFuture.class);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(scheduleWithId(1L));
        given(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).willReturn((ScheduledFuture) previous);
        scheduler.scheduleOnStartup();

        AdmissionSchedule changed = scheduleWithId(1L);
        LocalDateTime newTime = ScheduleFixture.SELECT_FIRST_PASS_AT.minusDays(1);
        ReflectionTestUtils.setField(changed, "selectFirstPassAt", newTime);
        given(admissionScheduleFacade.getSchedule(1L)).willReturn(changed);

        scheduler.reschedule(new AdmissionScheduleChangedEvent(1L));

        verify(previous).cancel(false);
        ArgumentCaptor<Instant> instants = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), instants.capture());
        assertEquals(toInstant(newTime), instants.getAllValues().get(1));
    }
}
