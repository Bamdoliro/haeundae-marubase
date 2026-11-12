package com.bamdoliro.maru.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 서버가 어느 타임존에서 뜨든 입학 일정은 한국 시간으로 판단한다.
 * 현재 시각이 필요한 곳은 이 Clock을 주입받아 쓰고, 테스트에서는 고정 Clock으로 바꿔 끼운다.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock serviceClock() {
        return Clock.system(SERVICE_ZONE_ID);
    }
}
