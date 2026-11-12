package com.bamdoliro.maru.domain.schedule.domain;

import com.bamdoliro.maru.domain.schedule.exception.InvalidSchedulePeriodException;
import com.bamdoliro.maru.shared.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 학년도별 입학 전형 일정. 활성(active) 일정은 하나만 두고, 프론트엔드는 활성 일정을 조회해 화면에 쓴다.
 * 동시 수정은 @Version(낙관적 락)으로 막는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_admission_schedule")
@Entity
public class AdmissionSchedule extends BaseTimeEntity {

    @Column(name = "schedule_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer admissionYear;

    @Column(nullable = false)
    private boolean active;

    // 원서 접수 시작
    @Column(nullable = false)
    private LocalDateTime applicationFormStart;

    // 방문 원서 접수 시작
    @Column(nullable = false)
    private LocalDateTime visitSubmissionAt;

    // 원서 접수 마감
    @Column(nullable = false)
    private LocalDateTime applicationFormEnd;

    // 1차 합격자 선발 실행 시각. 이 시각에 AdmissionScheduleTaskScheduler가 1차 선발을 돌린다.
    // 이미 저장된 일정 행이 있는 DB에서 ddl-auto: update로 NOT NULL 컬럼을 바로 추가하면 실패하므로
    // 컬럼은 nullable로 두고, 값이 반드시 있어야 한다는 검증은 validate()에서 한다.
    @Column(nullable = true)
    private LocalDateTime selectFirstPassAt;

    // 1차 합격 발표
    @Column(nullable = false)
    private LocalDateTime announcementOfFirstPass;

    // 2차 면접 시작
    @Column(nullable = false)
    private LocalDateTime interviewStart;

    // 2차 면접 종료
    @Column(nullable = false)
    private LocalDateTime interviewEnd;

    // 최종 합격 발표
    @Column(nullable = false)
    private LocalDateTime announcementOfFinalPass;

    // 입학 등록 시작
    @Column(nullable = false)
    private LocalDateTime registrationStart;

    // 입학 등록 마감
    @Column(nullable = false)
    private LocalDateTime registrationEnd;

    // 출결 기준일 (이 날까지의 출결을 반영)
    @Column(nullable = false)
    private LocalDate attendanceStandardDate;

    // 점검 시작 (선택값, 점검을 잡지 않으면 시작/종료 모두 null)
    @Column(nullable = true)
    private LocalDateTime maintenanceStart;

    // 점검 종료 (선택값)
    @Column(nullable = true)
    private LocalDateTime maintenanceEnd;

    @Version
    private Long version;

    @Builder
    public AdmissionSchedule(
            Integer admissionYear,
            boolean active,
            LocalDateTime applicationFormStart,
            LocalDateTime visitSubmissionAt,
            LocalDateTime applicationFormEnd,
            LocalDateTime selectFirstPassAt,
            LocalDateTime announcementOfFirstPass,
            LocalDateTime interviewStart,
            LocalDateTime interviewEnd,
            LocalDateTime announcementOfFinalPass,
            LocalDateTime registrationStart,
            LocalDateTime registrationEnd,
            LocalDate attendanceStandardDate,
            LocalDateTime maintenanceStart,
            LocalDateTime maintenanceEnd
    ) {
        this.active = active;
        apply(
                admissionYear,
                applicationFormStart,
                visitSubmissionAt,
                applicationFormEnd,
                selectFirstPassAt,
                announcementOfFirstPass,
                interviewStart,
                interviewEnd,
                announcementOfFinalPass,
                registrationStart,
                registrationEnd,
                attendanceStandardDate,
                maintenanceStart,
                maintenanceEnd
        );
    }

    public void update(
            Integer admissionYear,
            LocalDateTime applicationFormStart,
            LocalDateTime visitSubmissionAt,
            LocalDateTime applicationFormEnd,
            LocalDateTime selectFirstPassAt,
            LocalDateTime announcementOfFirstPass,
            LocalDateTime interviewStart,
            LocalDateTime interviewEnd,
            LocalDateTime announcementOfFinalPass,
            LocalDateTime registrationStart,
            LocalDateTime registrationEnd,
            LocalDate attendanceStandardDate,
            LocalDateTime maintenanceStart,
            LocalDateTime maintenanceEnd
    ) {
        apply(
                admissionYear,
                applicationFormStart,
                visitSubmissionAt,
                applicationFormEnd,
                selectFirstPassAt,
                announcementOfFirstPass,
                interviewStart,
                interviewEnd,
                announcementOfFinalPass,
                registrationStart,
                registrationEnd,
                attendanceStandardDate,
                maintenanceStart,
                maintenanceEnd
        );
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private void apply(
            Integer admissionYear,
            LocalDateTime applicationFormStart,
            LocalDateTime visitSubmissionAt,
            LocalDateTime applicationFormEnd,
            LocalDateTime selectFirstPassAt,
            LocalDateTime announcementOfFirstPass,
            LocalDateTime interviewStart,
            LocalDateTime interviewEnd,
            LocalDateTime announcementOfFinalPass,
            LocalDateTime registrationStart,
            LocalDateTime registrationEnd,
            LocalDate attendanceStandardDate,
            LocalDateTime maintenanceStart,
            LocalDateTime maintenanceEnd
    ) {
        validate(
                admissionYear,
                applicationFormStart,
                visitSubmissionAt,
                applicationFormEnd,
                selectFirstPassAt,
                announcementOfFirstPass,
                interviewStart,
                interviewEnd,
                announcementOfFinalPass,
                registrationStart,
                registrationEnd,
                attendanceStandardDate,
                maintenanceStart,
                maintenanceEnd
        );
        this.admissionYear = admissionYear;
        this.applicationFormStart = applicationFormStart;
        this.visitSubmissionAt = visitSubmissionAt;
        this.applicationFormEnd = applicationFormEnd;
        this.selectFirstPassAt = selectFirstPassAt;
        this.announcementOfFirstPass = announcementOfFirstPass;
        this.interviewStart = interviewStart;
        this.interviewEnd = interviewEnd;
        this.announcementOfFinalPass = announcementOfFinalPass;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.attendanceStandardDate = attendanceStandardDate;
        this.maintenanceStart = maintenanceStart;
        this.maintenanceEnd = maintenanceEnd;
    }

    /**
     * 시간 순서:
     * 원서 접수 시작 <= 방문 원서 접수 < 원서 접수 마감 < 1차 합격자 선발 <= 1차 합격 발표 < 면접 시작 < 면접 종료
     * < 최종 합격 발표 < 입학 등록 시작 < 입학 등록 마감
     * 출결 기준일은 원서 접수 마감일 이전(같은 날 포함)이어야 한다.
     * 점검 시작/종료는 선택값으로, 둘 다 없거나 둘 다 있으면서 점검 시작 < 점검 종료여야 한다.
     */
    private static void validate(
            Integer admissionYear,
            LocalDateTime applicationFormStart,
            LocalDateTime visitSubmissionAt,
            LocalDateTime applicationFormEnd,
            LocalDateTime selectFirstPassAt,
            LocalDateTime announcementOfFirstPass,
            LocalDateTime interviewStart,
            LocalDateTime interviewEnd,
            LocalDateTime announcementOfFinalPass,
            LocalDateTime registrationStart,
            LocalDateTime registrationEnd,
            LocalDate attendanceStandardDate,
            LocalDateTime maintenanceStart,
            LocalDateTime maintenanceEnd
    ) {
        boolean hasNull = !allNotNull(
                admissionYear,
                applicationFormStart,
                visitSubmissionAt,
                applicationFormEnd,
                selectFirstPassAt,
                announcementOfFirstPass,
                interviewStart,
                interviewEnd,
                announcementOfFinalPass,
                registrationStart,
                registrationEnd,
                attendanceStandardDate
        );
        if (hasNull) {
            throw new InvalidSchedulePeriodException();
        }

        boolean invalid = visitSubmissionAt.isBefore(applicationFormStart)
                || !visitSubmissionAt.isBefore(applicationFormEnd)
                || !applicationFormEnd.isBefore(selectFirstPassAt)
                || selectFirstPassAt.isAfter(announcementOfFirstPass)
                || !announcementOfFirstPass.isBefore(interviewStart)
                || !interviewStart.isBefore(interviewEnd)
                || !interviewEnd.isBefore(announcementOfFinalPass)
                || !announcementOfFinalPass.isBefore(registrationStart)
                || !registrationStart.isBefore(registrationEnd)
                || attendanceStandardDate.isAfter(applicationFormEnd.toLocalDate());

        if (invalid) {
            throw new InvalidSchedulePeriodException();
        }

        // 점검 시간은 선택값이라 필수값 검사와 시간 순서 체인에 넣지 않는다.
        // 둘 다 없거나, 둘 다 있으면서 시작이 종료보다 앞서야 한다.
        boolean invalidMaintenance = Objects.isNull(maintenanceStart) != Objects.isNull(maintenanceEnd)
                || (Objects.nonNull(maintenanceStart) && !maintenanceStart.isBefore(maintenanceEnd));

        if (invalidMaintenance) {
            throw new InvalidSchedulePeriodException();
        }
    }

    private static boolean allNotNull(Object... values) {
        for (Object value : values) {
            if (Objects.isNull(value)) {
                return false;
            }
        }
        return true;
    }
}
