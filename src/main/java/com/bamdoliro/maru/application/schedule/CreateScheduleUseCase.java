package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleAlreadyExistsException;
import com.bamdoliro.maru.infrastructure.persistence.schedule.AdmissionScheduleRepository;
import com.bamdoliro.maru.presentation.schedule.dto.request.ScheduleRequest;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.response.IdResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 새 학년도 일정을 등록하고 활성 일정으로 지정한다. 기존 활성 일정은 비활성으로 바꾼다.
 */
@RequiredArgsConstructor
@UseCase
public class CreateScheduleUseCase {

    private final AdmissionScheduleRepository admissionScheduleRepository;

    @Transactional
    public IdResponse execute(ScheduleRequest request) {
        if (admissionScheduleRepository.existsByAdmissionYear(request.getAdmissionYear())) {
            throw new ScheduleAlreadyExistsException();
        }

        AdmissionSchedule schedule = createSchedule(request);

        admissionScheduleRepository.findByActiveTrue().ifPresent(current -> {
            current.deactivate();
            admissionScheduleRepository.saveAndFlush(current);
        });

        return new IdResponse(admissionScheduleRepository.save(schedule).getId());
    }

    private AdmissionSchedule createSchedule(ScheduleRequest request) {
        return AdmissionSchedule.builder()
                .admissionYear(request.getAdmissionYear())
                .active(true)
                .applicationFormStart(request.getApplicationFormStart())
                .visitSubmissionAt(request.getVisitSubmissionAt())
                .applicationFormEnd(request.getApplicationFormEnd())
                .announcementOfFirstPass(request.getAnnouncementOfFirstPass())
                .interviewStart(request.getInterviewStart())
                .interviewEnd(request.getInterviewEnd())
                .announcementOfFinalPass(request.getAnnouncementOfFinalPass())
                .registrationStart(request.getRegistrationStart())
                .registrationEnd(request.getRegistrationEnd())
                .attendanceStandardDate(request.getAttendanceStandardDate())
                .build();
    }
}
