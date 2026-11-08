package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.domain.form.service.ExaminationNumberLock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Profile("!test")
@RequiredArgsConstructor
@Component
public class PostgresExaminationNumberLock implements ExaminationNumberLock {

    /**
     * 수험번호 부여 전용 advisory lock 키.
     * 이 프로젝트에서 다른 pg_advisory_lock을 쓰는 곳이 없고, 나중에 추가될 락과 겹치지 않도록
     * 의미 없는 임의의 상수(466_001)를 쓴다. 다른 락을 추가할 때는 이 값과 다른 키를 써야 한다.
     */
    static final long LOCK_KEY = 466_001L;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 트랜잭션 범위 advisory lock을 잡는다. 같은 트랜잭션에서 다시 호출해도 되고, 커밋/롤백 시 자동 해제된다.
     * 트랜잭션 밖에서 호출하면 락이 바로 풀려 의미가 없으므로 MANDATORY로 막는다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void lock() {
        jdbcTemplate.query("select pg_advisory_xact_lock(?)", (ResultSetExtractor<Void>) rs -> null, LOCK_KEY);
    }
}
