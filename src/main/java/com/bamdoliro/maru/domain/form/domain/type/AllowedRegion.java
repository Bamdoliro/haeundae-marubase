package com.bamdoliro.maru.domain.form.domain.type;

import com.bamdoliro.maru.shared.property.EnumProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum  AllowedRegion implements EnumProperty {
    BUSAN("부산광역시");

    private final String description;

    public static boolean isAllowed(String location) {
        if (location == null) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(region -> region.getDescription().equals(location));
    }
}
