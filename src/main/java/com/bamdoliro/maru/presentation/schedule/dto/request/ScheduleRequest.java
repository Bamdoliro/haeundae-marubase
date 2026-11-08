package com.bamdoliro.maru.presentation.schedule.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

    @NotNull(message = "필수값입니다.")
    @Min(value = 2000, message = "2000 이상이어야 합니다.")
    private Integer admissionYear;

    // 수정(PUT) 시 조회했던 일정의 version. 다르면 다른 관리자가 먼저 수정한 것으로 보고 409를 낸다.
    private Long version;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime applicationFormStart;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime visitSubmissionAt;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime applicationFormEnd;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime announcementOfFirstPass;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime interviewStart;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime interviewEnd;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime announcementOfFinalPass;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime registrationStart;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime registrationEnd;

    @NotNull(message = "필수값입니다.")
    private LocalDate attendanceStandardDate;
}
