package com.bamdoliro.maru.domain.schedule.domain;

import com.bamdoliro.maru.shared.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정별 1차 합격자 선발 실행 기록.
 * schedule_id에 unique 제약을 걸어, 여러 서버 인스턴스가 같은 시각에 선발을 실행해도 하나만 성공하게 한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_first_pass_selection_execution")
@Entity
public class FirstPassSelectionExecution extends BaseTimeEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false, unique = true)
    private AdmissionSchedule schedule;

    public FirstPassSelectionExecution(AdmissionSchedule schedule) {
        this.schedule = schedule;
    }
}
