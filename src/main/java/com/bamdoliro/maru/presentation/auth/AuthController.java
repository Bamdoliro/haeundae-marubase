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
import com.bamdoliro.maru.shared.response.CommonResponse;
import com.bamdoliro.maru.shared.response.SingleCommonResponse;
import com.bamdoliro.maru.shared.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인/재발급 응답은 쿠키와 바디 둘 다 내려주는 과도기 상태다.
 * 프론트엔드가 쿠키 + withCredentials로 전환하면 바디의 토큰과 Refresh-Token 헤더 읽기를 제거한다.
 */
@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {

    private static final String REFRESH_TOKEN_HEADER = "Refresh-Token";

    private final LogInUseCase logInUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogOutUseCase logOutUseCase;
    private final CookieUtil cookieUtil;

    @PostMapping
    @RateLimited(policy = RateLimitPolicy.LOGIN_ACCOUNT, key = "#p0.phoneNumber")
    public SingleCommonResponse<TokenResponse> logIn(
            @RequestBody @Valid LogInRequest request,
            HttpServletResponse response
    ) {
        TokenResponse tokenResponse = logInUseCase.execute(request);

        cookieUtil.addAccessTokenCookie(response, tokenResponse.getAccessToken());
        cookieUtil.addRefreshTokenCookie(response, tokenResponse.getRefreshToken());

        return CommonResponse.ok(tokenResponse);
    }

    @PatchMapping
    public SingleCommonResponse<TokenResponse> refreshToken(
            @CookieValue(value = CookieUtil.REFRESH_TOKEN, required = false) String refreshTokenCookie,
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshTokenHeader,
            HttpServletResponse response
    ) throws MissingRequestValueException {
        TokenResponse tokenResponse = refreshTokenUseCase.execute(
                resolveRefreshToken(refreshTokenCookie, refreshTokenHeader)
        );

        cookieUtil.addAccessTokenCookie(response, tokenResponse.getAccessToken());

        return CommonResponse.ok(tokenResponse);
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

    /**
     * refreshToken 쿠키를 먼저 쓰고, 없으면 Refresh-Token 헤더를 쓴다.
     * 둘 다 없으면 기존(헤더 누락)과 같은 400 응답을 낸다.
     */
    private String resolveRefreshToken(String cookie, String header) throws MissingRequestValueException {
        if (cookie != null && !cookie.isBlank()) {
            return cookie;
        }
        if (header != null && !header.isBlank()) {
            return header;
        }
        throw new MissingRequestValueException(
                "Required cookie '" + CookieUtil.REFRESH_TOKEN + "' or header '" + REFRESH_TOKEN_HEADER + "' is not present."
        );
    }
}
