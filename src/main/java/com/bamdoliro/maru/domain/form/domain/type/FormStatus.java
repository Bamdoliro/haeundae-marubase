package com.bamdoliro.maru.domain.form.domain.type;

import com.bamdoliro.maru.shared.property.EnumProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormStatus implements EnumProperty {
    SUBMITTED("제출됨"),
    FINAL_SUBMITTED("최종 제출됨"),
    ARRIVED("학교 도착"),
    NOT_ARRIVED("도착 안함"),
    APPROVED("승인됨"),
    REJECTED("반려됨"),
    RECEIVED("접수됨"),
    FIRST_PASSED("1차 합격"),
    FIRST_FAILED("1차 불합격"),
    PASSED("최종 합격"),
    FAILED("불합격"),
    NO_SHOW("불참"),
    ENTERED("입학");

    private final String description;
}
