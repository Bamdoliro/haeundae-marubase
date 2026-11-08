package com.bamdoliro.maru.shared.auth;

import com.bamdoliro.maru.domain.auth.domain.type.TokenType;
import com.bamdoliro.maru.domain.auth.service.TokenService;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.shared.config.properties.JwtProperties;
import com.bamdoliro.maru.shared.error.GlobalExceptionHandler;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationArgumentResolverTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    private TokenService tokenService;
    private MockMvc mockMvc;

    @RestController
    static class ProtectedController {

        @GetMapping("/protected")
        public String protectedEndpoint(@AuthenticationPrincipal User user) {
            return user.getName();
        }
    }

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setPrefix("Bearer");
        tokenService = mock(TokenService.class);

        AuthenticationArgumentResolver resolver = new AuthenticationArgumentResolver(
                new AuthenticationExtractor(jwtProperties),
                tokenService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new ProtectedController())
                .setCustomArgumentResolvers(resolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        given(tokenService.getType(ACCESS_TOKEN)).willReturn(TokenType.ACCESS_TOKEN.name());
        given(tokenService.getType(REFRESH_TOKEN)).willReturn(TokenType.REFRESH_TOKEN.name());
    }

    @Test
    void 헤더의_액세스_토큰으로_인증한다() throws Exception {
        User user = UserFixture.createUser();
        given(tokenService.getUser(ACCESS_TOKEN)).willReturn(user);

        mockMvc.perform(get("/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk());

        verify(tokenService).getUser(ACCESS_TOKEN);
    }

    @Test
    void 쿠키의_액세스_토큰으로_인증한다() throws Exception {
        User user = UserFixture.createUser();
        given(tokenService.getUser(ACCESS_TOKEN)).willReturn(user);

        mockMvc.perform(get("/protected")
                        .cookie(new Cookie("accessToken", ACCESS_TOKEN)))
                .andExpect(status().isOk());

        verify(tokenService).getUser(ACCESS_TOKEN);
    }

    @Test
    void 헤더의_리프레시_토큰으로_인증하면_401_에러가_발생한다() throws Exception {
        mockMvc.perform(get("/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + REFRESH_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        verify(tokenService, never()).getUser(anyString());
    }

    @Test
    void 쿠키의_리프레시_토큰으로_인증하면_401_에러가_발생한다() throws Exception {
        mockMvc.perform(get("/protected")
                        .cookie(new Cookie("accessToken", REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        verify(tokenService, never()).getUser(anyString());
    }

    @Test
    void 토큰_타입이_액세스가_아니면_401_에러가_발생한다() throws Exception {
        given(tokenService.getType("unknown-token")).willReturn(null);

        mockMvc.perform(get("/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer unknown-token"))
                .andExpect(status().isUnauthorized());

        verify(tokenService, never()).getUser(anyString());
    }
}
