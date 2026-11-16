package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.domain.form.domain.DocumentExportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentExportJobRepository extends JpaRepository<DocumentExportJob, Long> {
}
