package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.exception.InvalidSchedulePeriodException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleAlreadyExistsException;
import com.bamdoliro.maru.infrastructure.persistence.schedule.AdmissionScheduleRepository;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateScheduleUseCaseTest {

    @InjectMocks
    private CreateScheduleUseCase createScheduleUseCase;

    @Mock
    private AdmissionScheduleRepository admissionScheduleRepository;

    @Test
    void 일정을_등록하면_활성_일정이_되고_기존_활성_일정은_비활성이_된다() {
        AdmissionSchedule previous = ScheduleFixture.createSchedule(2026, true);
        given(admissionScheduleRepository.existsByAdmissionYear(2027)).willReturn(false);
        given(admissionScheduleRepository.findByActiveTrue()).willReturn(Optional.of(previous));
        given(admissionScheduleRepository.save(any(AdmissionSchedule.class))).willAnswer(invocation -> invocation.getArgument(0));

        createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null));

        assertFalse(previous.isActive());
        verify(admissionScheduleRepository).saveAndFlush(previous);
        ArgumentCaptor<AdmissionSchedule> captor = ArgumentCaptor.forClass(AdmissionSchedule.class);
        verify(admissionScheduleRepository).save(captor.capture());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void 같은_학년도_일정이_있으면_에러가_발생한다() {
        given(admissionScheduleRepository.existsByAdmissionYear(2027)).willReturn(true);

        assertThrows(ScheduleAlreadyExistsException.class,
                () -> createScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null)));
        verify(admissionScheduleRepository, never()).save(any());
    }

    @Test
    void 시간_순서가_잘못되면_기존_활성_일정을_건드리지_않고_에러가_발생한다() {
        given(admissionScheduleRepository.existsByAdmissionYear(2027)).willReturn(false);

        assertThrows(InvalidSchedulePeriodException.class,
                () -> createScheduleUseCase.execute(ScheduleFixture.createInvalidOrderRequest(2027)));
        verify(admissionScheduleRepository, never()).findByActiveTrue();
        verify(admissionScheduleRepository, never()).save(any());
    }
}
