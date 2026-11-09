package com.bamdoliro.maru.presentation.auth;

import com.bamdoliro.maru.application.auth.LogInUseCase;
import com.bamdoliro.maru.application.auth.LogOutUseCase;
import com.bamdoliro.maru.application.auth.RefreshTokenUseCase;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.presentation.auth.dto.request.LogInRequest;
import com.bamdoliro.maru.presentation.auth.dto.response.TokenResponse;
import com.bamdoliro.maru.shared.auth.AuthenticationPrincipal;
import com.bamdoliro.maru.shared.ratelimit.RateLimitPolicy;
import com.bamdoliro.maru.shared.ratelimit.RateLimited;
import com.bamdoliro.maru.shared.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인/재발급/로그아웃은 토큰을 HttpOnly 쿠키로만 주고받는다.
 * 유스케이스가 돌려주는 TokenResponse는 쿠키를 셋팅하는 데만 쓰고, 응답 바디로는 내려주지 않는다.
 */
@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {

    private final LogInUseCase logInUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogOutUseCase logOutUseCase;
    private final CookieUtil cookieUtil;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping
    @RateLimited(policy = RateLimitPolicy.LOGIN_ACCOUNT, key = "#p0.phoneNumber")
    public void logIn(
            @RequestBody @Valid LogInRequest request,
            HttpServletResponse response
    ) {
        TokenResponse tokenResponse = logInUseCase.execute(request);

        cookieUtil.addAccessTokenCookie(response, tokenResponse.getAccessToken());
        cookieUtil.addRefreshTokenCookie(response, tokenResponse.getRefreshToken());
    }

    /**
     * refreshToken 쿠키가 없으면 Spring이 MissingRequestCookieException(400)을 낸다.
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping
    public void refreshToken(
            @CookieValue(value = CookieUtil.REFRESH_TOKEN, required = true) String refreshToken,
            HttpServletResponse response
    ) {
        TokenResponse tokenResponse = refreshTokenUseCase.execute(refreshToken);

        cookieUtil.addAccessTokenCookie(response, tokenResponse.getAccessToken());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    public void logOut(
            @AuthenticationPrincipal User user,
            HttpServletResponse response
    ) {
        logOutUseCase.execute(user);

        cookieUtil.deleteAccessTokenCookie(response);
        cookieUtil.deleteRefreshTokenCookie(response);
    }
}
