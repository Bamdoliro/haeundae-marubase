package com.bamdoliro.maru.domain.user.exception;

import com.bamdoliro.maru.domain.user.exception.error.UserErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class UpdatePasswordTemporarilyUnavailableException extends MaruException {

    public UpdatePasswordTemporarilyUnavailableException() {
        super(UserErrorProperty.UPDATE_PASSWORD_TEMPORARILY_UNAVAILABLE);
    }
}
