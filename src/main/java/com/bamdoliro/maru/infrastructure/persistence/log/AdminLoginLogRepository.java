package com.bamdoliro.maru.infrastructure.persistence.log;

import com.bamdoliro.maru.domain.log.AdminLoginLog;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AdminLoginLogRepository extends CrudRepository<AdminLoginLog, Long> {

    /**
     * 테스트 데이터(isTestData = true 유저)를 참조하는 로그를 지운다.
     */
    @Modifying
    @Query("DELETE FROM AdminLoginLog l WHERE l.user.id IN (SELECT u.id FROM User u WHERE u.isTestData = true)")
    int deleteAllReferencingTestData();
}
