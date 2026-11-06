package com.bamdoliro.maru.application.form;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportFirstRoundResultDistrictTest {

    @ParameterizedTest
    @CsvSource(value = {
            "부산광역시 해운대구 해운대로 1 | 해운대구",
            "부산광역시 기장군 기장읍 기장대로 1 | 기장군",
            "전남광주통합특별시 북구 군왕로 234 | 북구",
            "경상남도 거제시 거제중앙로 1 | 거제시",
            "경상남도 창원시 의창구 의창대로 1 | 창원시 의창구",
            "충청북도 청주시 상당구 상당로 1 | 청주시 상당구",
            "충청북도 괴산군 괴산읍 1 | 괴산군",
            "제주특별자치도 서귀포시 중앙로 1 | 서귀포시",
            "전북특별자치도 김제시 성덕면 성동길 1 | 김제시",
            "세종특별자치시  만남로 65 | ",
            "부산광역시 | ",
    }, delimiter = '|')
    void 학교_주소에서_시군구를_추출한다(String address, String expected) {
        assertEquals(expected, ExportFirstRoundResultUseCase.extractDistrict(address));
    }

    @ParameterizedTest
    @CsvSource(value = {"''", "'   '"})
    void 주소가_비어있으면_시군구도_비운다(String address) {
        assertEquals(null, ExportFirstRoundResultUseCase.extractDistrict(address));
    }
}
