package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.domain.form.service.ExaminationNumberLock;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트 프로필(H2)에는 pg_advisory_xact_lock이 없으므로 아무 것도 하지 않는다.
 */
@Profile("test")
@Component
public class NoOpExaminationNumberLock implements ExaminationNumberLock {

    @Override
    public void lock() {
    }
}
