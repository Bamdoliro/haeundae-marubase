package com.bamdoliro.maru.shared.ratelimit;

import com.bamdoliro.maru.presentation.auth.dto.request.LogInRequest;
import com.bamdoliro.maru.presentation.auth.dto.response.TokenResponse;
import com.bamdoliro.maru.shared.fixture.AuthFixture;
import com.bamdoliro.maru.shared.util.ControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 컨트롤러에 RateLimitAspect를 붙여서, 어노테이션의 키가 요청 값으로 풀리고
 * 제한 초과가 HTTP 429 응답으로 내려가는지 확인한다.
 */
@Import(RateLimitAspect.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class RateLimitWebTest extends ControllerTest {

    @MockBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        // 다른 컨트롤러 테스트(RestDocsTestSupport)와 같은 방식으로 MockMvc를 구성한다.
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @Test
    void 로그인_요청마다_전화번호로_요청_횟수를_센다() throws Exception {
        LogInRequest request = new LogInRequest("01012345678", "비밀번호");
        given(logInUseCase.execute(any(LogInRequest.class)))
                .willReturn(new TokenResponse(AuthFixture.createAccessTokenString(), AuthFixture.createRefreshTokenString()));

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk());

        verify(rateLimitService, times(1)).consume(RateLimitPolicy.LOGIN_ACCOUNT, "01012345678");
    }

    @Test
    void 요청_횟수를_초과하면_429와_안내_메시지를_내려준다() throws Exception {
        LogInRequest request = new LogInRequest("01012345678", "비밀번호");
        willThrow(new RateLimitExceededException())
                .given(rateLimitService).consume(RateLimitPolicy.LOGIN_ACCOUNT, "01012345678");

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value("요청 횟수가 너무 많습니다. 잠시 후 다시 시도해 주세요."));

        verify(logInUseCase, never()).execute(any(LogInRequest.class));
    }
}
