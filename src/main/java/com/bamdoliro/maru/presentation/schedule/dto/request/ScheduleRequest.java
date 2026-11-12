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

    // 1차 합격자 선발 실행 시각. 원서 접수 마감보다 늦고 1차 합격 발표보다 이르거나 같아야 한다.
    @NotNull(message = "필수값입니다.")
    private LocalDateTime selectFirstPassAt;

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

    // 입학 등록원 및 서약서 제출 기간. 입학 등록 기간 안에 있어야 한다.
    @NotNull(message = "필수값입니다.")
    private LocalDateTime admissionAndPledgeStart;

    @NotNull(message = "필수값입니다.")
    private LocalDateTime admissionAndPledgeEnd;

    @NotNull(message = "필수값입니다.")
    private LocalDate attendanceStandardDate;

    // 점검 시간(선택값). 점검을 잡지 않으면 둘 다 null로 보낸다.
    private LocalDateTime maintenanceStart;

    private LocalDateTime maintenanceEnd;
}
