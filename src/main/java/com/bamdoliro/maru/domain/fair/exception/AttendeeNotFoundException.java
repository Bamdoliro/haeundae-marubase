package com.bamdoliro.maru.domain.fair.exception;

import com.bamdoliro.maru.domain.fair.exception.error.FairErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class AttendeeNotFoundException extends MaruException {

    public AttendeeNotFoundException() {
        super(FairErrorProperty.ATTENDEE_NOT_FOUND);
    }
}