package com.bamdoliro.maru.infrastructure.persistence.log;

import com.bamdoliro.maru.domain.log.AdminFormStatusChangeLog;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AdminFormStatusChangeLogRepository extends CrudRepository<AdminFormStatusChangeLog, Long> {

    /**
     * 테스트 데이터(isTestData = true 유저)의 원서나 유저를 참조하는 로그를 지운다.
     */
    @Modifying
    @Query("DELETE FROM AdminFormStatusChangeLog l WHERE l.form.id IN (SELECT f.id FROM Form f WHERE f.user.isTestData = true) OR l.user.id IN (SELECT u.id FROM User u WHERE u.isTestData = true)")
    int deleteAllReferencingTestData();
}
