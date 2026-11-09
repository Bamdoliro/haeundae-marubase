package com.bamdoliro.maru.presentation.schedule.dto.response;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ScheduleResponse {

    private final Long id;
    private final Integer admissionYear;
    private final boolean active;
    private final Long version;
    private final LocalDateTime applicationFormStart;
    private final LocalDateTime visitSubmissionAt;
    private final LocalDateTime applicationFormEnd;
    private final LocalDateTime announcementOfFirstPass;
    private final LocalDateTime interviewStart;
    private final LocalDateTime interviewEnd;
    private final LocalDateTime announcementOfFinalPass;
    private final LocalDateTime registrationStart;
    private final LocalDateTime registrationEnd;
    private final LocalDate attendanceStandardDate;
    private final LocalDateTime maintenanceStart;
    private final LocalDateTime maintenanceEnd;
    private final LocalDateTime updatedAt;

    public ScheduleResponse(AdmissionSchedule schedule) {
        this.id = schedule.getId();
        this.admissionYear = schedule.getAdmissionYear();
        this.active = schedule.isActive();
        this.version = schedule.getVersion();
        this.applicationFormStart = schedule.getApplicationFormStart();
        this.visitSubmissionAt = schedule.getVisitSubmissionAt();
        this.applicationFormEnd = schedule.getApplicationFormEnd();
        this.announcementOfFirstPass = schedule.getAnnouncementOfFirstPass();
        this.interviewStart = schedule.getInterviewStart();
        this.interviewEnd = schedule.getInterviewEnd();
        this.announcementOfFinalPass = schedule.getAnnouncementOfFinalPass();
        this.registrationStart = schedule.getRegistrationStart();
        this.registrationEnd = schedule.getRegistrationEnd();
        this.attendanceStandardDate = schedule.getAttendanceStandardDate();
        this.maintenanceStart = schedule.getMaintenanceStart();
        this.maintenanceEnd = schedule.getMaintenanceEnd();
        this.updatedAt = schedule.getUpdatedAt();
    }
}
