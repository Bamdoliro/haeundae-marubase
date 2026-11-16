package com.bamdoliro.maru.infrastructure.persistence.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.domain.FirstPassSelectionExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface FirstPassSelectionExecutionRepository extends JpaRepository<FirstPassSelectionExecution, Long> {

    boolean existsByScheduleAndExecutionTime(AdmissionSchedule schedule, LocalDateTime executionTime);
}
