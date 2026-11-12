package com.bamdoliro.maru.shared.config;

import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 서비스 시각을 ScheduleFixture의 원서 접수 기간 안으로 고정한다.
 * 원서 접수 기간 검사(ScheduleValidationAspect)를 거치는 통합 테스트에서 활성 일정과 함께 쓴다.
 */
@TestConfiguration
public class FixedClockConfig {

    public static final LocalDateTime NOW = ScheduleFixture.APPLICATION_FORM_START.plusDays(1);

    @Primary
    @Bean
    public Clock fixedServiceClock() {
        return Clock.fixed(NOW.atZone(TimeConfig.SERVICE_ZONE_ID).toInstant(), TimeConfig.SERVICE_ZONE_ID);
    }
}
