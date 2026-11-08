package com.bamdoliro.maru.domain.form.domain.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedRegionTest {

    // NEIS schoolInfo API의 LCTN_SC_NM 값 기준
    @ParameterizedTest
    @ValueSource(strings = {
            "부산광역시",
            "전남광주통합특별시(광주)",
            "경상남도",
            "충청북도",
            "제주특별자치도",
            "세종특별자치시"
    })
    void 허용된_지역의_학교는_검색된다(String location) {
        assertTrue(AllowedRegion.isAllowed(location));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "광주광역시",
            "전남광주통합특별시(전남)",
            "경기도",
            "서울특별시"
    })
    void 허용되지_않은_지역의_학교는_검색되지_않는다(String location) {
        assertFalse(AllowedRegion.isAllowed(location));
    }

    @Test
    void 지역명이_없으면_검색되지_않는다() {
        assertFalse(AllowedRegion.isAllowed(null));
    }
}
