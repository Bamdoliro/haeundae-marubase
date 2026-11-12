package com.bamdoliro.maru.shared.fixture;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.presentation.schedule.dto.request.ScheduleRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 시간 순서가 올바른 입학 일정 예시.
 */
public class ScheduleFixture {

    public static final LocalDateTime APPLICATION_FORM_START = LocalDateTime.of(2026, 10, 20, 9, 0);
    public static final LocalDateTime VISIT_SUBMISSION_AT = LocalDateTime.of(2026, 10, 27, 9, 0);
    public static final LocalDateTime APPLICATION_FORM_END = LocalDateTime.of(2026, 10, 30, 17, 0);
    public static final LocalDateTime SELECT_FIRST_PASS_AT = LocalDateTime.of(2026, 12, 9, 21, 20);
    public static final LocalDateTime ANNOUNCEMENT_OF_FIRST_PASS = LocalDateTime.of(2026, 12, 11, 15, 0);
    public static final LocalDateTime INTERVIEW_START = LocalDateTime.of(2026, 12, 15, 9, 0);
    public static final LocalDateTime INTERVIEW_END = LocalDateTime.of(2026, 12, 15, 17, 0);
    public static final LocalDateTime ANNOUNCEMENT_OF_FINAL_PASS = LocalDateTime.of(2026, 12, 24, 12, 0);
    public static final LocalDateTime REGISTRATION_START = LocalDateTime.of(2027, 1, 5, 9, 0);
    public static final LocalDateTime REGISTRATION_END = LocalDateTime.of(2027, 1, 8, 17, 0);
    public static final LocalDate ATTENDANCE_STANDARD_DATE = LocalDate.of(2026, 10, 1);
    public static final LocalDateTime MAINTENANCE_START = LocalDateTime.of(2026, 10, 25, 0, 0);
    public static final LocalDateTime MAINTENANCE_END = LocalDateTime.of(2026, 10, 25, 6, 0);

    public static AdmissionSchedule createSchedule(int admissionYear, boolean active) {
        return createSchedule(admissionYear, active, null, null);
    }

    /**
     * 점검 시간이 잡혀 있는 일정.
     */
    public static AdmissionSchedule createScheduleWithMaintenance(int admissionYear, boolean active) {
        return createSchedule(admissionYear, active, MAINTENANCE_START, MAINTENANCE_END);
    }

    private static AdmissionSchedule createSchedule(
            int admissionYear,
            boolean active,
            LocalDateTime maintenanceStart,
            LocalDateTime maintenanceEnd
    ) {
        return AdmissionSchedule.builder()
                .admissionYear(admissionYear)
                .active(active)
                .applicationFormStart(APPLICATION_FORM_START)
                .visitSubmissionAt(VISIT_SUBMISSION_AT)
                .applicationFormEnd(APPLICATION_FORM_END)
                .selectFirstPassAt(SELECT_FIRST_PASS_AT)
                .announcementOfFirstPass(ANNOUNCEMENT_OF_FIRST_PASS)
                .interviewStart(INTERVIEW_START)
                .interviewEnd(INTERVIEW_END)
                .announcementOfFinalPass(ANNOUNCEMENT_OF_FINAL_PASS)
                .registrationStart(REGISTRATION_START)
                .registrationEnd(REGISTRATION_END)
                .attendanceStandardDate(ATTENDANCE_STANDARD_DATE)
                .maintenanceStart(maintenanceStart)
                .maintenanceEnd(maintenanceEnd)
                .build();
    }

    public static ScheduleRequest createRequest(int admissionYear, Long version) {
        return createRequest(admissionYear, version, REGISTRATION_END);
    }

    /**
     * 입학 등록 마감만 바꾼 수정 요청. 값이 실제로 바뀌어야 version이 올라간다.
     */
    public static ScheduleRequest createRequest(int admissionYear, Long version, LocalDateTime registrationEnd) {
        return new ScheduleRequest(
                admissionYear,
                version,
                APPLICATION_FORM_START,
                VISIT_SUBMISSION_AT,
                APPLICATION_FORM_END,
                SELECT_FIRST_PASS_AT,
                ANNOUNCEMENT_OF_FIRST_PASS,
                INTERVIEW_START,
                INTERVIEW_END,
                ANNOUNCEMENT_OF_FINAL_PASS,
                REGISTRATION_START,
                registrationEnd,
                ATTENDANCE_STANDARD_DATE,
                null,
                null
        );
    }

    /**
     * 원서 접수 마감이 시작보다 빠른 잘못된 요청.
     */
    public static ScheduleRequest createInvalidOrderRequest(int admissionYear) {
        return createInvalidOrderRequest(admissionYear, null);
    }

    public static ScheduleRequest createInvalidOrderRequest(int admissionYear, Long version) {
        return new ScheduleRequest(
                admissionYear,
                version,
                APPLICATION_FORM_END,
                VISIT_SUBMISSION_AT,
                APPLICATION_FORM_START,
                SELECT_FIRST_PASS_AT,
                ANNOUNCEMENT_OF_FIRST_PASS,
                INTERVIEW_START,
                INTERVIEW_END,
                ANNOUNCEMENT_OF_FINAL_PASS,
                REGISTRATION_START,
                REGISTRATION_END,
                ATTENDANCE_STANDARD_DATE,
                null,
                null
        );
    }
}
