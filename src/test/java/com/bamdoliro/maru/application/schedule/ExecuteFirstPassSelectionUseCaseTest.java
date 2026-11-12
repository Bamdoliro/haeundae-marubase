package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.application.form.SelectFirstPassUseCase;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.domain.FirstPassSelectionExecution;
import com.bamdoliro.maru.domain.schedule.exception.FirstPassAlreadyExecutedException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.persistence.schedule.FirstPassSelectionExecutionRepository;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
    void 실행_기록을_남기고_1차_합격자를_선발한다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        given(admissionScheduleFacade.getSchedule(1L)).willReturn(schedule);
        given(executionRepository.existsBySchedule(schedule)).willReturn(false);

        executeFirstPassSelectionUseCase.execute(1L);

        verify(executionRepository).saveAndFlush(any(FirstPassSelectionExecution.class));
        verify(selectFirstPassUseCase).execute();
    }

    @Test
    void 이미_실행된_일정이면_선발하지_않고_에러가_발생한다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        given(admissionScheduleFacade.getSchedule(1L)).willReturn(schedule);
        given(executionRepository.existsBySchedule(schedule)).willReturn(true);

        assertThrows(FirstPassAlreadyExecutedException.class, () -> executeFirstPassSelectionUseCase.execute(1L));
        verify(executionRepository, never()).saveAndFlush(any());
        verify(selectFirstPassUseCase, never()).execute();
    }
}
