package com.bamdoliro.maru.presentation.schedule;

import com.bamdoliro.maru.domain.schedule.exception.InvalidSchedulePeriodException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleVersionConflictException;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.presentation.schedule.dto.request.ScheduleRequest;
import com.bamdoliro.maru.shared.fixture.AuthFixture;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import com.bamdoliro.maru.shared.response.IdResponse;
import com.bamdoliro.maru.shared.util.RestDocsTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminScheduleControllerTest extends RestDocsTestSupport {

    @BeforeEach
    void setUpAdmin() {
        User admin = UserFixture.createAdminUser();
        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(admin);
    }

    @Test
    void 어드민이_현재_입학_일정을_조회한다() throws Exception {
        given(queryCurrentScheduleUseCase.execute()).willReturn(ScheduleControllerTest.createResponse());

        mockMvc.perform(get("/admin/schedules/current")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(0))

                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer token")
                        )
                ));
    }

    @Test
    void 어드민이_학년도로_입학_일정을_조회한다() throws Exception {
        given(queryScheduleUseCase.execute(2027)).willReturn(ScheduleControllerTest.createResponse());

        mockMvc.perform(get("/admin/schedules")
                        .param("admission-year", "2027")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isOk())

                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("admission-year")
                                        .description("학년도")
                        )
                ));
    }

    @Test
    void 어드민이_입학_일정을_등록한다() throws Exception {
        ScheduleRequest request = ScheduleFixture.createRequest(2027, null);
        given(createScheduleUseCase.execute(any(ScheduleRequest.class))).willReturn(new IdResponse(1L));

        mockMvc.perform(post("/admin/schedules")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))

                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("admissionYear").type(JsonFieldType.NUMBER).description("학년도"),
                                fieldWithPath("version").type(JsonFieldType.NUMBER).optional()
                                        .description("수정 시 조회한 일정의 version (등록 시 생략)"),
                                fieldWithPath("applicationFormStart").type(JsonFieldType.STRING).description("원서 접수 시작"),
                                fieldWithPath("visitSubmissionAt").type(JsonFieldType.STRING).description("방문 원서 접수 시작"),
                                fieldWithPath("applicationFormEnd").type(JsonFieldType.STRING).description("원서 접수 마감"),
                                fieldWithPath("announcementOfFirstPass").type(JsonFieldType.STRING).description("1차 합격 발표"),
                                fieldWithPath("interviewStart").type(JsonFieldType.STRING).description("2차 면접 시작"),
                                fieldWithPath("interviewEnd").type(JsonFieldType.STRING).description("2차 면접 종료"),
                                fieldWithPath("announcementOfFinalPass").type(JsonFieldType.STRING).description("최종 합격 발표"),
                                fieldWithPath("registrationStart").type(JsonFieldType.STRING).description("입학 등록 시작"),
                                fieldWithPath("registrationEnd").type(JsonFieldType.STRING).description("입학 등록 마감"),
                                fieldWithPath("attendanceStandardDate").type(JsonFieldType.STRING).description("출결 기준일 (yyyy-MM-dd)"),
                                fieldWithPath("maintenanceStart").type(JsonFieldType.STRING).optional()
                                        .description("점검 시작 (선택, 점검 종료와 함께 보내거나 둘 다 생략)"),
                                fieldWithPath("maintenanceEnd").type(JsonFieldType.STRING).optional()
                                        .description("점검 종료 (선택, 점검 시작보다 늦어야 함)")
                        )
                ));

        verify(createScheduleUseCase, times(1)).execute(any(ScheduleRequest.class));
    }

    @Test
    void 필수값이_없으면_입학_일정을_등록할_수_없다() throws Exception {
        ScheduleRequest request = new ScheduleRequest();

        mockMvc.perform(post("/admin/schedules")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isBadRequest())

                .andDo(restDocs.document());

        verify(createScheduleUseCase, never()).execute(any(ScheduleRequest.class));
    }

    @Test
    void 시간_순서가_잘못된_입학_일정은_400을_반환한다() throws Exception {
        ScheduleRequest request = ScheduleFixture.createInvalidOrderRequest(2027);
        willThrow(new InvalidSchedulePeriodException()).given(createScheduleUseCase).execute(any(ScheduleRequest.class));

        mockMvc.perform(post("/admin/schedules")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_PERIOD"))

                .andDo(restDocs.document());
    }

    @Test
    void 어드민이_현재_입학_일정을_수정한다() throws Exception {
        ScheduleRequest request = ScheduleFixture.createRequest(2027, 0L);

        mockMvc.perform(put("/admin/schedules/current")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isNoContent())

                .andDo(restDocs.document());

        verify(updateScheduleUseCase, times(1)).execute(any(ScheduleRequest.class));
    }

    @Test
    void 다른_관리자가_먼저_수정했다면_409를_반환한다() throws Exception {
        ScheduleRequest request = ScheduleFixture.createRequest(2027, 0L);
        willThrow(new ScheduleVersionConflictException()).given(updateScheduleUseCase).execute(any(ScheduleRequest.class));

        mockMvc.perform(put("/admin/schedules/current")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_VERSION_CONFLICT"))

                .andDo(restDocs.document());
    }
}
