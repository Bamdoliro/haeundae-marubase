package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleAlreadyExistsException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleNotFoundException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleVersionConflictException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.persistence.schedule.AdmissionScheduleRepository;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateScheduleUseCaseTest {

    @InjectMocks
    private UpdateScheduleUseCase updateScheduleUseCase;

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    @Mock
    private AdmissionScheduleRepository admissionScheduleRepository;

    @Test
    void 현재_일정을_수정한다() {
        // 저장 전 엔티티의 version은 null이다.
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);

        updateScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null));

        verify(admissionScheduleRepository).saveAndFlush(schedule);
    }

    @Test
    void 요청의_version이_저장된_version과_다르면_에러가_발생한다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);

        assertThrows(ScheduleVersionConflictException.class,
                () -> updateScheduleUseCase.execute(ScheduleFixture.createRequest(2027, 3L)));
        verify(admissionScheduleRepository, never()).saveAndFlush(any());
    }

    @Test
    void 같은_version으로_동시에_수정해서_저장이_충돌하면_버전_충돌_에러로_바꾼다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);
        willThrow(new ObjectOptimisticLockingFailureException(AdmissionSchedule.class, 1L))
                .given(admissionScheduleRepository).saveAndFlush(schedule);

        assertThrows(ScheduleVersionConflictException.class,
                () -> updateScheduleUseCase.execute(ScheduleFixture.createRequest(2027, null)));
    }

    @Test
    void 이미_다른_일정이_있는_학년도로_바꾸면_에러가_발생한다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);
        given(admissionScheduleRepository.existsByAdmissionYear(2026)).willReturn(true);

        assertThrows(ScheduleAlreadyExistsException.class,
                () -> updateScheduleUseCase.execute(ScheduleFixture.createRequest(2026, null)));
    }

    @Test
    void 활성_일정이_없으면_에러가_발생한다() {
        given(admissionScheduleFacade.getCurrentSchedule()).willThrow(new ScheduleNotFoundException());

        assertThrows(ScheduleNotFoundException.class,
                () -> updateScheduleUseCase.execute(ScheduleFixture.createRequest(2027, 0L)));
    }
}
