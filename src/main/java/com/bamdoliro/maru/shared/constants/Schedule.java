package com.bamdoliro.maru.shared.constants;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@UtilityClass
public class Schedule {

    public static final LocalDateTime START = LocalDateTime.of(2026, 9, 21, 16, 0);
    public static final LocalDateTime END = LocalDateTime.of(2026, 9, 24, 23, 59);
    public static final LocalDateTime ANNOUNCEMENT_OF_FIRST_PASS = LocalDateTime.of(2026, 12, 11, 15, 0);
    public static final LocalDateTime ANNOUNCEMENT_OF_SECOND_PASS = LocalDateTime.of(2026, 12, 24, 12, 0);
    public static final LocalDateTime DEPTH_INTERVIEW = LocalDateTime.of(2026, 12, 15, 9, 0);
    public static final LocalDateTime ENTRANCE_REGISTRATION_PERIOD_START = LocalDateTime.of(2027, 1, 5, 0, 0);
    public static final LocalDateTime ENTRANCE_REGISTRATION_PERIOD_END = LocalDateTime.of(2027, 1, 8, 0, 0);
    public static final LocalDateTime ADMISSION_AND_PLEDGE_START = LocalDateTime.of(2027, 1, 5, 0, 0);
    public static final LocalDateTime ADMISSION_AND_PLEDGE_END = LocalDateTime.of(2027, 1, 7, 23, 59);

    public static final String SELECT_FIRST_PASS_CRON = "0 20 21 9 12 ?";

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
