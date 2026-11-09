package com.bamdoliro.maru.presentation.auth;

import com.bamdoliro.maru.shared.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이미 깨진 세션의 인증 쿠키를 정리한다.
 * 재발급 실패나 로그아웃 실패처럼 유저를 특정할 수 없는 상황에서 부르므로 인증을 요구하지 않고,
 * 로그아웃 유스케이스(DB의 리프레시 토큰 삭제)도 실행하지 않는다.
 * 쿠키를 구운 CookieUtil이 같은 Domain/Path로 지우므로 브라우저에서 같은 쿠키로 취급된다.
 */
@RequiredArgsConstructor
@RequestMapping("/session")
@RestController
public class SessionController {

    private final CookieUtil cookieUtil;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/clear")
    public void clear(HttpServletResponse response) {
        cookieUtil.deleteAccessTokenCookie(response);
        cookieUtil.deleteRefreshTokenCookie(response);
    }
}
