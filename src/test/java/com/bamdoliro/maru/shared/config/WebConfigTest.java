package com.bamdoliro.maru.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebConfigTest {

    private WebConfig webConfigWith(String... allowedOrigins) {
        WebConfig webConfig = new WebConfig(null);
        ReflectionTestUtils.setField(webConfig, "allowedOrigins", allowedOrigins);
        return webConfig;
    }

    @Test
    void 설정된_origin으로_CORS를_허용한다() {
        WebConfig webConfig = webConfigWith("https://hhsapply.com", " https://madmin.hhsapply.com ");

        assertDoesNotThrow(() -> webConfig.addCorsMappings(new CorsRegistry()));
    }

    @Test
    void 쿠키를_허용하므로_와일드카드_origin은_거부한다() {
        WebConfig webConfig = webConfigWith("https://hhsapply.com", "*");

        assertThrows(IllegalStateException.class, () -> webConfig.addCorsMappings(new CorsRegistry()));
    }

    @Test
    void origin이_비어_있으면_거부한다() {
        WebConfig webConfig = webConfigWith("");

        assertThrows(IllegalStateException.class, () -> webConfig.addCorsMappings(new CorsRegistry()));
    }
}
