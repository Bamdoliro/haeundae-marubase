package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.application.form.SelectFirstPassUseCase;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.domain.FirstPassSelectionExecution;
import com.bamdoliro.maru.domain.schedule.exception.FirstPassAlreadyExecutedException;
import com.bamdoliro.maru.domain.schedule.exception.FirstPassScheduleChangedException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.persistence.schedule.FirstPassSelectionExecutionRepository;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExecuteFirstPassSelectionUseCaseTest {

    @InjectMocks
    private ExecuteFirstPassSelectionUseCase executeFirstPassSelectionUseCase;

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    @Mock
    private FirstPassSelectionExecutionRepository executionRepository;

    @Mock
    private SelectFirstPassUseCase selectFirstPassUseCase;

    @Test
    void 실행_기록을_예약_시각과_함께_남기고_1차_합격자를_선발한다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        LocalDateTime executionTime = schedule.getSelectFirstPassAt();
        given(admissionScheduleFacade.getSchedule(1L)).willReturn(schedule);
        given(executionRepository.existsByScheduleAndExecutionTime(schedule, executionTime)).willReturn(false);

        executeFirstPassSelectionUseCase.execute(1L, executionTime);

        ArgumentCaptor<FirstPassSelectionExecution> saved = ArgumentCaptor.forClass(FirstPassSelectionExecution.class);
        verify(executionRepository).saveAndFlush(saved.capture());
        assertSame(schedule, saved.getValue().getSchedule());
        assertEquals(executionTime, saved.getValue().getExecutionTime());
        verify(selectFirstPassUseCase).execute();
    }

    @Test
    void 같은_일정_같은_시각으로_이미_실행했으면_선발하지_않고_에러가_발생한다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        LocalDateTime executionTime = schedule.getSelectFirstPassAt();
        given(admissionScheduleFacade.getSchedule(1L)).willReturn(schedule);
        given(executionRepository.existsByScheduleAndExecutionTime(schedule, executionTime)).willReturn(true);

        assertThrows(FirstPassAlreadyExecutedException.class,
                () -> executeFirstPassSelectionUseCase.execute(1L, executionTime));
        verify(executionRepository, never()).saveAndFlush(any());
        verify(selectFirstPassUseCase, never()).execute();
    }

    @Test
    void 같은_일정이라도_선발_시각을_바꿔_다시_예약하면_새_시각으로_다시_선발한다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        LocalDateTime firstTime = schedule.getSelectFirstPassAt();
        LocalDateTime changedTime = firstTime.plusHours(3);
        given(admissionScheduleFacade.getSchedule(1L)).willReturn(schedule);
        given(executionRepository.existsByScheduleAndExecutionTime(schedule, firstTime)).willReturn(false);
        executeFirstPassSelectionUseCase.execute(1L, firstTime);

        ReflectionTestUtils.setField(schedule, "selectFirstPassAt", changedTime);
        given(executionRepository.existsByScheduleAndExecutionTime(schedule, changedTime)).willReturn(false);
        executeFirstPassSelectionUseCase.execute(1L, changedTime);

        ArgumentCaptor<FirstPassSelectionExecution> saved = ArgumentCaptor.forClass(FirstPassSelectionExecution.class);
        verify(executionRepository, times(2)).saveAndFlush(saved.capture());
        assertEquals(firstTime, saved.getAllValues().get(0).getExecutionTime());
        assertEquals(changedTime, saved.getAllValues().get(1).getExecutionTime());
        verify(selectFirstPassUseCase, times(2)).execute();
    }

    @Test
    void 일정의_선발_시각과_다른_옛_예약이면_선발하지_않고_에러가_발생한다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        LocalDateTime staleTime = schedule.getSelectFirstPassAt().minusHours(1);
        given(admissionScheduleFacade.getSchedule(1L)).willReturn(schedule);

        assertThrows(FirstPassScheduleChangedException.class,
                () -> executeFirstPassSelectionUseCase.execute(1L, staleTime));
        verify(executionRepository, never()).existsByScheduleAndExecutionTime(any(), any());
        verify(executionRepository, never()).saveAndFlush(any());
        verify(selectFirstPassUseCase, never()).execute();
    }
}
