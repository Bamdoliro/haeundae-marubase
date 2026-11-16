package com.bamdoliro.maru.domain.schedule.exception;

import com.bamdoliro.maru.domain.schedule.exception.error.ScheduleErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class FirstPassScheduleChangedException extends MaruException {

    public FirstPassScheduleChangedException() {
        super(ScheduleErrorProperty.FIRST_PASS_SCHEDULE_CHANGED);
    }
}
