package com.bamdoliro.maru.infrastructure.persistence.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdmissionScheduleRepository extends JpaRepository<AdmissionSchedule, Long> {

    Optional<AdmissionSchedule> findByActiveTrue();

    Optional<AdmissionSchedule> findByAdmissionYear(Integer admissionYear);

    boolean existsByAdmissionYear(Integer admissionYear);
}
