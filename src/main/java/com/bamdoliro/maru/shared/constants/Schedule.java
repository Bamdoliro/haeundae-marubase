package com.bamdoliro.maru.shared.constants;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@UtilityClass
public class Schedule {

    // 서버가 어느 타임존에서 뜨든 전형 일정은 한국 시간으로 판단한다
    public static final String ZONE_ID = "Asia/Seoul";
    public static final ZoneId ZONE = ZoneId.of(ZONE_ID);

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public String toLocaleString(LocalDateTime datetime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm", Locale.KOREA);
        return formatter.format(datetime);
    }

    public String toShortLocaleString(LocalDateTime datetime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M. d.(E) HH:mm", Locale.KOREA);
        return formatter.format(datetime);
    }

    public String toLocaleString(LocalDateTime startTime, LocalDateTime endTime) {
        DateTimeFormatter startTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREA);
        DateTimeFormatter endTimeFormatter = DateTimeFormatter.ofPattern(" ~ MM.dd (E)", Locale.KOREA);
        return startTimeFormatter.format(startTime) + endTimeFormatter.format(endTime);
    }
}
