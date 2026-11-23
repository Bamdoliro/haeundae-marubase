package com.bamdoliro.maru.domain.form.exception;

import com.bamdoliro.maru.domain.form.exception.error.FormErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class DocumentExportInProgressException extends MaruException {

    public DocumentExportInProgressException() {
        super(FormErrorProperty.DOCUMENT_EXPORT_IN_PROGRESS);
    }
}
