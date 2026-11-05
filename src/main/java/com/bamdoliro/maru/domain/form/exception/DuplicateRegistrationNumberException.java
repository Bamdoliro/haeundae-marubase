package com.bamdoliro.maru.domain.form.exception;

import com.bamdoliro.maru.domain.form.exception.error.FormErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class DuplicateRegistrationNumberException extends MaruException {

    public DuplicateRegistrationNumberException() {
        super(FormErrorProperty.DUPLICATE_REGISTRATION_NUMBER);
    }
}
