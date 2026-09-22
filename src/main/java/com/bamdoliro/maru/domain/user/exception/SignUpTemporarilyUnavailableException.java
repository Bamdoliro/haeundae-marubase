package com.bamdoliro.maru.domain.user.exception;

import com.bamdoliro.maru.domain.user.exception.error.UserErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class SignUpTemporarilyUnavailableException extends MaruException {

    public SignUpTemporarilyUnavailableException() {
        super(UserErrorProperty.SIGN_UP_TEMPORARILY_UNAVAILABLE);
    }
}
