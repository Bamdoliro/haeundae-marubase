package com.bamdoliro.maru.infrastructure.persistence.log;

import com.bamdoliro.maru.domain.log.UpdatedField;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UpdatedFieldRepository extends CrudRepository<UpdatedField, Long> {

    /**
     * 테스트 데이터(isTestData = true 유저)의 원서 수정 로그에 딸린 변경 필드를 지운다. FormUpdateLog보다 먼저 지워야 한다.
     */
    @Modifying
    @Query("DELETE FROM UpdatedField l WHERE l.formUpdateLog.id IN (SELECT fl.id FROM FormUpdateLog fl WHERE fl.form.id IN (SELECT f.id FROM Form f WHERE f.user.isTestData = true) OR fl.user.id IN (SELECT u.id FROM User u WHERE u.isTestData = true))")
    int deleteAllReferencingTestData();
}
