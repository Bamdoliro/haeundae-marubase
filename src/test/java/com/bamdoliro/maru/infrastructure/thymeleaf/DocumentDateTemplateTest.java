package com.bamdoliro.maru.infrastructure.thymeleaf;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 서류 서명란 날짜는 템플릿이 현재 시각을 직접 읽지 않고, 출력하는 쪽에서 넘긴 documentDate로 찍는다.
 */
@ActiveProfiles("test")
@SpringBootTest
class DocumentDateTemplateTest {

    @Autowired
    private ProcessTemplateService processTemplateService;

    @Test
    void 지원자가_출력할_때_넘긴_날짜가_서명란에_찍힌다() {
        String html = render(LocalDate.of(2026, 12, 8));

        assertTrue(html.contains("2026년 12월 08일"));
    }

    @Test
    void 관리자가_출력할_때_넘긴_최종_제출_시각의_날짜가_서명란에_찍힌다() {
        String html = render(LocalDateTime.of(2026, 12, 9, 23, 59));

        assertTrue(html.contains("2026년 12월 09일"));
    }

    @Test
    void 제출_시각이_없는_원서도_날짜만_비우고_렌더링된다() {
        String html = render(null);

        assertFalse(html.contains("2025년 11월 12일"));
    }

    private String render(Object documentDate) {
        Form form = FormFixture.createForm(FormType.REGULAR);
        Map<String, Object> data = new HashMap<>();
        data.put("form", form);
        data.put("year", 2027);
        data.put("documentDate", documentDate);
        return processTemplateService.execute(Templates.DOCUMENT_ONE, data);
    }
}
