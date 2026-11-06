package com.bamdoliro.maru.domain.form.domain.type;

import com.bamdoliro.maru.shared.property.EnumProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum  AllowedRegion implements EnumProperty {
    BUSAN("부산광역시"),
    GWANGJU("광주광역시"),
    GYENGNAM("경상남도"),
    CHUNGBUK("충청북도"),
    JEJU("제주특별자치도"),
    SEJONG("세종특별자치시");

    private final String description;

    public static boolean isAllowed(String location) {
        if (location == null) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(region -> region.getDescription().equals(location));
    }
}
