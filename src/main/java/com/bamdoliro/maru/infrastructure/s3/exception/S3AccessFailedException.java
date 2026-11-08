package com.bamdoliro.maru.infrastructure.s3.exception;

import com.bamdoliro.maru.infrastructure.s3.exception.error.S3ErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class S3AccessFailedException extends MaruException {

    public S3AccessFailedException() {
        super(S3ErrorProperty.ACCESS_FAILED);
    }
}
