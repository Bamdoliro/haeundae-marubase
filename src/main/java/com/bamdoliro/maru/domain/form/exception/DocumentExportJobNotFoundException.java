package com.bamdoliro.maru.domain.form.exception;

import com.bamdoliro.maru.domain.form.exception.error.FormErrorProperty;
import com.bamdoliro.maru.shared.error.MaruException;

public class DocumentExportJobNotFoundException extends MaruException {

    public DocumentExportJobNotFoundException() {
        super(FormErrorProperty.DOCUMENT_EXPORT_JOB_NOT_FOUND);
    }
}
