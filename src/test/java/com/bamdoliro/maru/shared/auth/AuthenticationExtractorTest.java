package com.bamdoliro.maru.shared.auth;

import com.bamdoliro.maru.domain.auth.exception.EmptyTokenException;
import com.bamdoliro.maru.domain.auth.exception.InvalidTokenException;
import com.bamdoliro.maru.shared.config.properties.JwtProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationExtractorTest {

    private AuthenticationExtractor authenticationExtractor;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setPrefix("Bearer");
        authenticationExtractor = new AuthenticationExtractor(jwtProperties);
    }

    @Test
    void 쿠키의_액세스_토큰만으로_인증한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", "cookie-token"));

        assertEquals("cookie-token", authenticationExtractor.extract(new ServletWebRequest(request)));
    }

    @Test
    void 쿠키가_없으면_Authorization_헤더로_인증한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");

        assertEquals("header-token", authenticationExtractor.extract(new ServletWebRequest(request)));
    }

    @Test
    void 쿠키와_헤더가_모두_있으면_쿠키를_쓴다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", "cookie-token"));
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");

        assertEquals("cookie-token", authenticationExtractor.extract(new ServletWebRequest(request)));
    }

    @Test
    void 액세스_토큰_쿠키가_비어_있으면_헤더로_인증한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", ""), new Cookie("refreshToken", "refresh-token"));
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");

        assertEquals("header-token", authenticationExtractor.extract(new ServletWebRequest(request)));
    }

    @Test
    void 쿠키와_헤더가_모두_없으면_에러가_발생한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "refresh-token"));

        assertThrows(EmptyTokenException.class, () -> authenticationExtractor.extract(new ServletWebRequest(request)));
    }

    @Test
    void 헤더의_접두사가_다르면_에러가_발생한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic header-token");

        assertThrows(InvalidTokenException.class, () -> authenticationExtractor.extract(new ServletWebRequest(request)));
    }
}
