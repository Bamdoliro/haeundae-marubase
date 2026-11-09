package com.bamdoliro.maru.presentation.auth;

import com.bamdoliro.maru.shared.fixture.AuthFixture;
import com.bamdoliro.maru.shared.util.RestDocsTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.responseCookies;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionControllerTest extends RestDocsTestSupport {

    /**
     * Set-Cookie 헤더 중 name 쿠키가 값이 비어 있고 Max-Age=0이며, 쿠키를 구울 때와 같은 속성을 갖는지 확인한다.
     */
    private static ResultMatcher expiredCookie(String name) {
        return result -> {
            List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
            String cookie = cookies.stream()
                    .filter(value -> value.startsWith(name + "="))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(name + " 쿠키가 없습니다. Set-Cookie=" + cookies));
            for (String part : List.of(name + "=;", "Max-Age=0", "HttpOnly", "Secure", "SameSite=Strict", "Path=/", "Domain=hhsapply.com")) {
                assertTrue(cookie.contains(part), name + " 쿠키에 " + part + "가 없습니다: " + cookie);
            }
        };
    }

    @Test
    void 인증_쿠키를_만료시킨다() throws Exception {
        mockMvc.perform(post("/session/clear")
                        .cookie(new Cookie("accessToken", AuthFixture.createAccessTokenString()),
                                new Cookie("refreshToken", AuthFixture.createRefreshTokenString()))
                )

                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(expiredCookie("accessToken"))
                .andExpect(expiredCookie("refreshToken"))

                .andDo(restDocs.document(
                        responseCookies(
                                cookieWithName("accessToken")
                                        .description("빈 값, Max-Age=0으로 만료"),
                                cookieWithName("refreshToken")
                                        .description("빈 값, Max-Age=0으로 만료")
                        )
                ));
    }

    @Test
    void 인증_정보가_없어도_인증_쿠키를_만료시킨다() throws Exception {
        mockMvc.perform(post("/session/clear"))

                .andExpect(status().isNoContent())
                .andExpect(expiredCookie("accessToken"))
                .andExpect(expiredCookie("refreshToken"))

                .andDo(restDocs.document());

        verify(authenticationArgumentResolver, never()).resolveArgument(any(), any(), any(), any());
        verify(logOutUseCase, never()).execute(any());
    }
}
