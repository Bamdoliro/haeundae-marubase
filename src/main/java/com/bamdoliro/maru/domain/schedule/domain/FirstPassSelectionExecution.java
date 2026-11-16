package com.bamdoliro.maru.domain.schedule.domain;

import com.bamdoliro.maru.shared.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 일정별 1차 합격자 선발 실행 기록.
 * (schedule_id, execution_time)에 unique 제약을 걸어, 여러 서버 인스턴스가 같은 예약 시각에 선발을 실행해도 하나만 성공하게 한다.
 * 일정 수정은 같은 일정 row를 갱신하므로, 선발 시각을 바꿔 다시 예약하면 새 시각으로 한 번 더 실행할 수 있다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tbl_first_pass_selection_execution",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_first_pass_selection_execution_schedule_time",
                columnNames = {"schedule_id", "execution_time"}
        )
)
@Entity
public class FirstPassSelectionExecution extends BaseTimeEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private AdmissionSchedule schedule;

    /**
     * 선발이 예약된 시각(실행 당시 일정의 selectFirstPassAt).
     * ddl-auto: update로 기존 행이 있는 테이블에 NOT NULL 컬럼을 추가하려면 DB 기본값이 필요해 columnDefinition을 둔다.
     * 기존 행은 기본값(1970-01-01 00:00:00)으로 채워지며, 새 기록은 항상 실제 예약 시각을 저장한다.
     */
    @Column(name = "execution_time", nullable = false, columnDefinition = "timestamp(6) default '1970-01-01 00:00:00'")
    private LocalDateTime executionTime;

    public FirstPassSelectionExecution(AdmissionSchedule schedule, LocalDateTime executionTime) {
        this.schedule = schedule;
        this.executionTime = executionTime;
    }
}
