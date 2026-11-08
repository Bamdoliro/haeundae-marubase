package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleAlreadyExistsException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleVersionConflictException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.persistence.schedule.AdmissionScheduleRepository;
import com.bamdoliro.maru.presentation.schedule.dto.request.ScheduleRequest;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 현재 활성 일정을 수정한다.
 * 요청의 version이 저장된 version과 다르면(다른 관리자가 먼저 수정) 409를 낸다.
 * 같은 version으로 동시에 들어온 경우에도 @Version 충돌을 409로 바꾼다.
 */
@RequiredArgsConstructor
@UseCase
public class UpdateScheduleUseCase {

    private final AdmissionScheduleFacade admissionScheduleFacade;
    private final AdmissionScheduleRepository admissionScheduleRepository;

    @Transactional
    public void execute(ScheduleRequest request) {
        AdmissionSchedule schedule = admissionScheduleFacade.getCurrentSchedule();
        validateVersion(schedule, request.getVersion());
        validateAdmissionYear(schedule, request.getAdmissionYear());

        schedule.update(
                request.getAdmissionYear(),
                request.getApplicationFormStart(),
                request.getVisitSubmissionAt(),
                request.getApplicationFormEnd(),
                request.getAnnouncementOfFirstPass(),
                request.getInterviewStart(),
                request.getInterviewEnd(),
                request.getAnnouncementOfFinalPass(),
                request.getRegistrationStart(),
                request.getRegistrationEnd(),
                request.getAttendanceStandardDate()
        );

        try {
            admissionScheduleRepository.saveAndFlush(schedule);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ScheduleVersionConflictException();
        }
    }

    private void validateVersion(AdmissionSchedule schedule, Long version) {
        if (!Objects.equals(schedule.getVersion(), version)) {
            throw new ScheduleVersionConflictException();
        }
    }

    private void validateAdmissionYear(AdmissionSchedule schedule, Integer admissionYear) {
        if (!Objects.equals(schedule.getAdmissionYear(), admissionYear)
                && admissionScheduleRepository.existsByAdmissionYear(admissionYear)) {
            throw new ScheduleAlreadyExistsException();
        }
    }
}
