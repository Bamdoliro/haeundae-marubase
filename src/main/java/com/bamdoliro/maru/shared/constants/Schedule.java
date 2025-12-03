package com.bamdoliro.maru.shared.constants;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@UtilityClass
public class Schedule {

    public static final LocalDateTime START = LocalDateTime.of(2025, 12, 5, 0, 0);
    public static final LocalDateTime END = LocalDateTime.of(2025, 12, 10, 17, 0);
    public static final LocalDateTime ANNOUNCEMENT_OF_FIRST_PASS = LocalDateTime.of(2025, 12, 12, 15, 0);
    public static final LocalDateTime ANNOUNCEMENT_OF_SECOND_PASS = LocalDateTime.of(2025, 12, 26, 12, 0);
    public static final LocalDateTime DEPTH_INTERVIEW = LocalDateTime.of(2025, 12, 16, 9, 0);
    public static final LocalDateTime ENTRANCE_REGISTRATION_PERIOD_START = LocalDateTime.of(2026, 1, 6, 0, 0);
    public static final LocalDateTime ENTRANCE_REGISTRATION_PERIOD_END = LocalDateTime.of(2026, 1, 9, 0, 0);
    public static final LocalDateTime ADMISSION_AND_PLEDGE_START = LocalDateTime.of(2026, 1, 6, 0, 0);
    public static final LocalDateTime ADMISSION_AND_PLEDGE_END = LocalDateTime.of(2026, 1, 8, 23, 59);

    public static final String SELECT_FIRST_PASS_CRON = "0 55 9 26 12 ?";

    public static int getAdmissionYear() {
        return START.plusYears(1L).getYear();
    }

    public String toLocaleString(LocalDateTime datetime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm", Locale.KOREA);
        return formatter.format(datetime);
    }

    public String toLocaleString(LocalDateTime startTime, LocalDateTime endTime) {
        DateTimeFormatter startTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREA);
        DateTimeFormatter endTimeFormatter = DateTimeFormatter.ofPattern(" ~ MM.dd (E)", Locale.KOREA);
        return startTimeFormatter.format(startTime) + endTimeFormatter.format(endTime);
    }
}
