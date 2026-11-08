package com.bamdoliro.maru.shared.util;

import com.bamdoliro.maru.shared.config.properties.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 인증 토큰을 HttpOnly 쿠키로 내려주고 지운다.
 * 쿠키 만료 시간은 토큰 만료 시간(jwt.*-expiration-time)과 같게 맞춘다.
 */
@RequiredArgsConstructor
@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";

    private final JwtProperties jwtProperties;

    @Value("${domain.name}")
    private String domain;

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        addCookie(response, buildCookie(ACCESS_TOKEN, accessToken, Duration.ofMillis(jwtProperties.getAccessExpirationTime())));
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        addCookie(response, buildCookie(REFRESH_TOKEN, refreshToken, Duration.ofMillis(jwtProperties.getRefreshExpirationTime())));
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        addCookie(response, buildCookie(ACCESS_TOKEN, "", Duration.ZERO));
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        addCookie(response, buildCookie(REFRESH_TOKEN, "", Duration.ZERO));
    }

    private void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .domain(domain)
                .maxAge(maxAge)
                .path("/")
                .build();
    }
}
