package com.bamdoliro.maru.domain.form.domain.type;

import com.bamdoliro.maru.shared.property.EnumProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentExportJobStatus implements EnumProperty {
    PENDING("대기 중"),
    IN_PROGRESS("생성 중"),
    COMPLETED("완료"),
    FAILED("실패"),
    ;

    private final String description;
}
