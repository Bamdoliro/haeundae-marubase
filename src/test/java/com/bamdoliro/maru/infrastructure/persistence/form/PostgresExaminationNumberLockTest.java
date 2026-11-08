package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.domain.form.service.ExaminationNumberLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(PostgresExaminationNumberLockTest.Config.class)
class PostgresExaminationNumberLockTest {

    @Autowired
    private ExaminationNumberLock examinationNumberLock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 트랜잭션_없이_락을_잡으면_에러가_발생한다() {
        clearInvocations(jdbcTemplate);

        assertThrows(IllegalTransactionStateException.class, () -> examinationNumberLock.lock());
        verify(jdbcTemplate, never()).query(anyString(), any(ResultSetExtractor.class), any(Object[].class));
    }

    @Test
    void 트랜잭션_안에서는_트랜잭션_범위_advisory_lock을_잡는다() {
        clearInvocations(jdbcTemplate);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> examinationNumberLock.lock());

        verify(jdbcTemplate).query(
                eq("select pg_advisory_xact_lock(?)"),
                any(ResultSetExtractor.class),
                eq(PostgresExaminationNumberLock.LOCK_KEY)
        );
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new DataSourceTransactionManager(new DriverManagerDataSource("jdbc:h2:mem:examination-number-lock"));
        }

        @Bean
        PostgresExaminationNumberLock postgresExaminationNumberLock(JdbcTemplate jdbcTemplate) {
            return new PostgresExaminationNumberLock(jdbcTemplate);
        }
    }
}
