package com.bamdoliro.maru.presentation.auth;

import com.bamdoliro.maru.domain.auth.exception.ExpiredTokenException;
import com.bamdoliro.maru.domain.auth.exception.InvalidTokenException;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.user.exception.PasswordMismatchException;
import com.bamdoliro.maru.domain.user.exception.UserNotFoundException;
import com.bamdoliro.maru.presentation.auth.dto.request.LogInRequest;
import com.bamdoliro.maru.presentation.auth.dto.response.TokenResponse;
import com.bamdoliro.maru.shared.fixture.AuthFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import com.bamdoliro.maru.shared.util.RestDocsTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.options;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends RestDocsTestSupport {

    @BeforeEach
    void setUpTokenExpiration() {
        // 쿠키 만료 시간은 jwt 설정값(1시간, 15일)을 따른다.
        given(jwtProperties.getAccessExpirationTime()).willReturn(3600000L);
        given(jwtProperties.getRefreshExpirationTime()).willReturn(1296000000L);
    }

    /**
     * Set-Cookie 헤더 중 name으로 시작하는 쿠키가 있고, 공통 속성과 기대 조각을 모두 포함하는지 확인한다.
     */
    private static ResultMatcher setCookie(String name, String... expectedParts) {
        return result -> {
            List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
            String cookie = cookies.stream()
                    .filter(value -> value.startsWith(name + "="))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(name + " 쿠키가 없습니다. Set-Cookie=" + cookies));
            for (String part : List.of("HttpOnly", "Secure", "SameSite=Strict", "Path=/", "Domain=hhsapply.com")) {
                assertTrue(cookie.contains(part), name + " 쿠키에 " + part + " 속성이 없습니다: " + cookie);
            }
            for (String part : expectedParts) {
                assertTrue(cookie.contains(part), name + " 쿠키에 " + part + "가 없습니다: " + cookie);
            }
        };
    }

    @Test
    void 유저가_로그인한다() throws Exception {
        User user = UserFixture.createUser();
        LogInRequest request = new LogInRequest(user.getPhoneNumber(), "비밀번호");
        TokenResponse response = new TokenResponse(AuthFixture.createAccessTokenString(), AuthFixture.createRefreshTokenString());

        given(logInUseCase.execute(any(LogInRequest.class))).willReturn(response);

        mockMvc.perform(post("/auth")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isOk())
                // 과도기: 기존 프론트엔드를 위해 바디에도 토큰을 내려준다.
                .andExpect(jsonPath("$.data.accessToken").value(response.getAccessToken()))
                .andExpect(jsonPath("$.data.refreshToken").value(response.getRefreshToken()))
                .andExpect(setCookie("accessToken", "accessToken=" + response.getAccessToken(), "Max-Age=3600"))
                .andExpect(setCookie("refreshToken", "refreshToken=" + response.getRefreshToken(), "Max-Age=1296000"))

                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("phoneNumber")
                                        .type(JsonFieldType.STRING)
                                        .description("전화번호"),
                                fieldWithPath("password")
                                        .type(JsonFieldType.STRING)
                                        .description("비밀번호")
                        )
                ));
    }

    @Test
    void 유저가_로그인할_때_유저가_없으면_에러가_발생한다() throws Exception {
        User user = UserFixture.createUser();
        LogInRequest request = new LogInRequest(user.getPhoneNumber(), "비밀번호");
        doThrow(new UserNotFoundException()).when(logInUseCase).execute(any(LogInRequest.class));

        mockMvc.perform(post("/auth")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isNotFound())

                .andDo(restDocs.document());
    }

    @Test
    void 유저가_로그인할_때_비밀번호가_틀리면_에러가_발생한다() throws Exception {
        User user = UserFixture.createUser();
        LogInRequest request = new LogInRequest(user.getPhoneNumber(), "비밀번호");
        doThrow(new PasswordMismatchException()).when(logInUseCase).execute(any(LogInRequest.class));

        mockMvc.perform(post("/auth")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isUnauthorized())

                .andDo(restDocs.document());
    }

    @Test
    void 유저가_로그인할_때_잘못된_형식의_요청을_보내면_에러가_발생한다() throws Exception {
        LogInRequest request = new LogInRequest("", "");

        mockMvc.perform(post("/auth")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isBadRequest())

                .andDo(restDocs.document());

        verify(logInUseCase, never()).execute(any(LogInRequest.class));
    }

    @Test
    void 리프레시_토큰으로_액세스_토큰을_재발급한다() throws Exception {
        String refreshToken = AuthFixture.createRefreshTokenString();
        TokenResponse response = TokenResponse.builder().accessToken(AuthFixture.createAccessTokenString()).build();
        given(refreshTokenUseCase.execute(refreshToken)).willReturn(response);

        mockMvc.perform(patch("/auth")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Refresh-Token", refreshToken)
                )

                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data.accessToken").value(response.getAccessToken()))
                .andExpect(setCookie("accessToken", "accessToken=" + response.getAccessToken(), "Max-Age=3600"))

                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName("Refresh-Token")
                                        .description("리프레시 토큰")
                        )
                ));
    }

    @Test
    void 만료된_리프레시_토큰으로_액세스_토큰_재발급을_요청하면_에러가_발생한다() throws Exception {
        String expiredRefreshToken = AuthFixture.createRefreshTokenString();
        doThrow(new ExpiredTokenException()).when(refreshTokenUseCase).execute(expiredRefreshToken);

        mockMvc.perform(patch("/auth")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Refresh-Token", expiredRefreshToken)
                )

                .andExpect(status().isUnauthorized())

                .andDo(restDocs.document());
    }

    @Test
    void 액세스_토큰으로_액세스_토큰_재발급을_요청하면_에러가_발생한다() throws Exception {
        String accessToken = AuthFixture.createRefreshTokenString();
        doThrow(new InvalidTokenException()).when(refreshTokenUseCase).execute(accessToken);

        mockMvc.perform(patch("/auth")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Refresh-Token", accessToken)
                )

                .andExpect(status().isUnauthorized())

                .andDo(restDocs.document());
    }

    @Test
    void 액세스_토큰을_재발급할_때_리프레시_토큰을_보내지_않으면_에러가_발생한다() throws Exception {

        mockMvc.perform(patch("/auth")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isBadRequest())

                .andDo(restDocs.document());

        verify(refreshTokenUseCase, never()).execute(anyString());
    }

    @Test
    void 토큰의_서명_알고리즘이_불일치하면_에러가_발생한다() throws Exception {
        String accessToken = AuthFixture.createAccessTokenString();
        doThrow(new InvalidTokenException()).when(refreshTokenUseCase).execute(accessToken);

        mockMvc.perform(patch("/auth")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Refresh-Token", accessToken)
                )

                .andExpect(status().isUnauthorized())

                .andDo(restDocs.document());
    }

    @Test
    void 유저가_로그아웃한다() throws Exception {
        User user = UserFixture.createUser();

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        willDoNothing().given(logOutUseCase).execute(user);

        mockMvc.perform(delete("/auth")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isNoContent())
                .andExpect(setCookie("accessToken", "accessToken=;", "Max-Age=0"))
                .andExpect(setCookie("refreshToken", "refreshToken=;", "Max-Age=0"))

                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer token")
                        )
                ));

        verify(logOutUseCase, times(1)).execute(user);
    }

    @Test
    void 리프레시_토큰_쿠키로_액세스_토큰을_재발급한다() throws Exception {
        String refreshToken = AuthFixture.createRefreshTokenString();
        TokenResponse response = TokenResponse.builder().accessToken(AuthFixture.createAccessTokenString()).build();
        given(refreshTokenUseCase.execute(refreshToken)).willReturn(response);

        mockMvc.perform(patch("/auth")
                        .cookie(new Cookie("refreshToken", refreshToken))
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(response.getAccessToken()))
                .andExpect(setCookie("accessToken", "accessToken=" + response.getAccessToken(), "Max-Age=3600"))

                .andDo(restDocs.document());

        verify(refreshTokenUseCase, times(1)).execute(refreshToken);
    }

    @Test
    void 리프레시_토큰_쿠키와_헤더가_모두_있으면_쿠키를_쓴다() throws Exception {
        String cookieRefreshToken = AuthFixture.createRefreshTokenString();
        TokenResponse response = TokenResponse.builder().accessToken(AuthFixture.createAccessTokenString()).build();
        given(refreshTokenUseCase.execute(cookieRefreshToken)).willReturn(response);

        mockMvc.perform(patch("/auth")
                        .cookie(new Cookie("refreshToken", cookieRefreshToken))
                        .header("Refresh-Token", "header-refresh-token")
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isOk())

                .andDo(restDocs.document());

        verify(refreshTokenUseCase, times(1)).execute(cookieRefreshToken);
        verify(refreshTokenUseCase, never()).execute("header-refresh-token");
    }

    @Test
    void 허용된_origin의_CORS_요청은_쿠키를_주고받을_수_있다() throws Exception {
        mockMvc.perform(options("/auth")
                        .header(HttpHeaders.ORIGIN, "https://hhsapply.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                )

                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://hhsapply.com"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void 허용되지_않은_origin의_CORS_요청은_거부한다() throws Exception {
        mockMvc.perform(options("/auth")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                )

                .andExpect(status().isForbidden());
    }
}
