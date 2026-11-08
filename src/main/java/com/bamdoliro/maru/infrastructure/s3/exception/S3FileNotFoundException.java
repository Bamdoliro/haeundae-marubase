package com.bamdoliro.maru.infrastructure.s3.exception;

import com.bamdoliro.maru.infrastructure.s3.exception.error.S3ErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class S3FileNotFoundException extends MaruException {

    public S3FileNotFoundException() {
        super(S3ErrorProperty.FILE_NOT_FOUND);
    }
}
