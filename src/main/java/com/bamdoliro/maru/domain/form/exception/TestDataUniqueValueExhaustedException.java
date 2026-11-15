package com.bamdoliro.maru.domain.form.exception;

import com.bamdoliro.maru.domain.form.exception.error.FormErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class TestDataUniqueValueExhaustedException extends MaruException {

    public TestDataUniqueValueExhaustedException() {
        super(FormErrorProperty.TEST_DATA_UNIQUE_VALUE_EXHAUSTED);
    }
}
