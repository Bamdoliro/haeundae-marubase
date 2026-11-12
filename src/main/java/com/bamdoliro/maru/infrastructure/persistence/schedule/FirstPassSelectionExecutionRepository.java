package com.bamdoliro.maru.infrastructure.persistence.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.domain.FirstPassSelectionExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FirstPassSelectionExecutionRepository extends JpaRepository<FirstPassSelectionExecution, Long> {

    boolean existsBySchedule(AdmissionSchedule schedule);
}
