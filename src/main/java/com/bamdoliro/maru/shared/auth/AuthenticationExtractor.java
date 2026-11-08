package com.bamdoliro.maru.shared.auth;

import com.bamdoliro.maru.domain.auth.exception.EmptyTokenException;
import com.bamdoliro.maru.domain.auth.exception.InvalidTokenException;
import com.bamdoliro.maru.shared.config.properties.JwtProperties;
import com.bamdoliro.maru.shared.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

@RequiredArgsConstructor
@Component
public class AuthenticationExtractor {

    private final JwtProperties jwtProperties;

    /**
     * accessToken 쿠키를 먼저 읽고, 없으면 Authorization 헤더(Bearer)를 읽는다.
     * 프론트엔드가 쿠키 방식으로 전환하는 동안 두 방식을 모두 받는다.
     */
    public String extract(NativeWebRequest request) {
        String cookieToken = extractFromCookie(request);
        if (cookieToken != null) {
            return cookieToken;
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new EmptyTokenException();
        }

        if (!authorizationHeader.startsWith(jwtProperties.getPrefix())) {
            throw new InvalidTokenException();
        }

        return authorizationHeader.replace(jwtProperties.getPrefix(), "").trim();
    }

    private String extractFromCookie(NativeWebRequest request) {
        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
        if (servletRequest == null || servletRequest.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : servletRequest.getCookies()) {
            if (CookieUtil.ACCESS_TOKEN.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
