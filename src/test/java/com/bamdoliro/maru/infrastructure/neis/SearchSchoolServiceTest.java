package com.bamdoliro.maru.infrastructure.neis;

import com.bamdoliro.maru.presentation.school.dto.response.SchoolResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
class SearchSchoolServiceTest {

    @Autowired
    private SearchSchoolService searchSchoolService;

    @Test
    void 영남을_검색하면_부산의_영남중학교만을_반환한다() throws JsonProcessingException {
        String q = "영남";
        List<SchoolResponse> responseList = searchSchoolService.execute(q);
        assertEquals(1, responseList.size());
        assertEquals("영남중학교", responseList.get(0).getName());
    }

    @Test
    void 검색_결과가_많다면_상위_10개까지만_반환한다() throws JsonProcessingException {
        String q = "중학교";
        List<SchoolResponse> responseList = searchSchoolService.execute(q);
        assertTrue(responseList.size() <= 10);
    }

    @Test
    void 검색_결과가_없다면_빈_리스트를_반환한다() throws JsonProcessingException {
        String q = "누가봐도없을것같은검색어";
        List<SchoolResponse> responseList = searchSchoolService.execute(q);
        assertEquals(0, responseList.size());
    }
}