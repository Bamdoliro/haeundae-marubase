package com.bamdoliro.maru.presentation.fair;

import com.bamdoliro.maru.domain.fair.domain.type.FairType;
import com.bamdoliro.maru.domain.fair.exception.FairNotFoundException;
import com.bamdoliro.maru.domain.fair.exception.HeadcountExceededException;
import com.bamdoliro.maru.domain.fair.exception.NotApplicationPeriodException;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.presentation.fair.dto.request.*;
import com.bamdoliro.maru.shared.fixture.AuthFixture;
import com.bamdoliro.maru.shared.fixture.FairFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import com.bamdoliro.maru.shared.util.RestDocsTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FairControllerTest extends RestDocsTestSupport {

    @Test
    void 입학설명회_일정을_만든다() throws Exception {
        CreateFairRequest request = FairFixture.createFairRequest();
        User user = UserFixture.createAdminUser();

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        willDoNothing().given(createAdmissionFairUseCase).execute(request);

        mockMvc.perform(post("/fairs")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isCreated())

                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer token")
                        ),
                        requestFields(
                                fieldWithPath("start")
                                        .type(JsonFieldType.STRING)
                                        .description("입학설명회 일시 (yyyy-MM-ddThh:mm:ss)"),
                                fieldWithPath("capacity")
                                        .type(JsonFieldType.NUMBER)
                                        .description("입학설명회 정원"),
                                fieldWithPath("place")
                                        .type(JsonFieldType.STRING)
                                        .description("입학설명회 장소"),
                                fieldWithPath("type")
                                        .type(JsonFieldType.STRING)
                                        .description("<<fair-type,입학설명회 유형>>"),
                                fieldWithPath("applicationStartDate")
                                        .type(JsonFieldType.STRING)
                                        .optional()
                                        .description("입학설명회 신청 시작일 (yyyy-MM-dd)"),
                                fieldWithPath("applicationEndDate")
                                        .type(JsonFieldType.STRING)
                                        .optional()
                                        .description("입학설명회 신청 종료일 (yyyy-MM-dd)")
                        )
                ));

        verify(createAdmissionFairUseCase, times(1)).execute(any(CreateFairRequest.class));
    }

    @Test
    void 입학설명회의_정보를_수정한다() throws Exception {
        Long fairId = 1L;
        User user = UserFixture.createAdminUser();
        UpdateFairRequest request = FairFixture.updateFairRequest();

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        willDoNothing().given(updateAdmissionFairUseCase).execute(fairId ,request);

        mockMvc.perform(put("/fairs/{fair-id}", fairId)
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isNoContent())

                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer token")
                        ),
                        pathParameters(
                                parameterWithName("fair-id")
                                        .description("입학설명회 id")
                        ),
                        requestFields(
                                fieldWithPath("start")
                                        .type(JsonFieldType.STRING)
                                        .description("입학설명회 일시 (yyyy-MM-ddThh:mm:ss)"),
                                fieldWithPath("capacity")
                                        .type(JsonFieldType.NUMBER)
                                        .description("입학설명회 정원"),
                                fieldWithPath("place")
                                        .type(JsonFieldType.STRING)
                                        .description("입학설명회 장소"),
                                fieldWithPath("type")
                                        .type(JsonFieldType.STRING)
                                        .description("<<fair-type,입학설명회 유형>>"),
                                fieldWithPath("applicationStartDate")
                                        .type(JsonFieldType.STRING)
                                        .optional()
                                        .description("입학설명회 신청 시작일 (yyyy-MM-dd)"),
                                fieldWithPath("applicationEndDate")
                                        .type(JsonFieldType.STRING)
                                        .optional()
                                        .description("입학설명회 신청 종료일 (yyyy-MM-dd)")
                        )
                ));

        verify(updateAdmissionFairUseCase, times(1)).execute(eq(fairId), any(UpdateFairRequest.class));
    }

    @Test
    void 입학설명회의_정보를_수정할_때_입학설명회가_없다면_오류가_발생한다()throws Exception {
        Long fairId = 2L;
        User user = UserFixture.createAdminUser();
        UpdateFairRequest request = FairFixture.updateFairRequest();
        doThrow(new FairNotFoundException()).when(updateAdmissionFairUseCase).execute(any(Long.class), any(UpdateFairRequest.class));

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        willDoNothing().given(updateAdmissionFairUseCase).execute(fairId, request);

        mockMvc.perform(put("/fairs/{fair-id}", fairId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isNotFound())

                .andDo(restDocs.document());

        verify(updateAdmissionFairUseCase, times(1)).execute(eq(fairId), any(UpdateFairRequest.class));
    }

    @Test
    void 입학설명회를_삭제한다() throws Exception {
        Long fairId = 1L;
        willDoNothing().given(deleteAdmissionFairUseCase).execute(fairId);

        User user = UserFixture.createAdminUser();
        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);

        mockMvc.perform(delete("/fairs/{fair-id}", fairId)
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isNoContent())

                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer token")
                        ),
                        pathParameters(
                                parameterWithName("fair-id")
                                        .description("입학섦여회 id")
                        )
                ));

        verify(deleteAdmissionFairUseCase, times(1)).execute(fairId);
    }

    @Test
    void 입학설명회에_참가_신청을_한다() throws Exception {
        Long fairId = 1L;
        AttendAdmissionFairRequest request = FairFixture.createAttendAdmissionFairRequest();
        willDoNothing().given(attendAdmissionFairUseCase).execute(any(Long.class), any(AttendAdmissionFairRequest.class));

        mockMvc.perform(post("/fairs/{fair-id}", fairId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isCreated())

                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("fair-id")
                                        .description("입학설명회 id")
                        ),
                        requestFields(
                                fieldWithPath("schoolName")
                                        .type(JsonFieldType.STRING)
                                        .description("신청자 학교 이름"),
                                fieldWithPath("name")
                                        .type(JsonFieldType.STRING)
                                        .description("신청자 이름"),
                                fieldWithPath("type")
                                        .type(JsonFieldType.STRING)
                                        .description("신청자 유형 (학생, 학부모, 교사, 기타등등...)"),
                                fieldWithPath("phoneNumber")
                                        .type(JsonFieldType.STRING)
                                        .description("신청자 전화번호"),
                                fieldWithPath("headcount")
                                        .type(JsonFieldType.NUMBER)
                                        .description("신청자 인원수"),
                                fieldWithPath("question")
                                        .type(JsonFieldType.STRING)
                                        .optional()
                                        .description("신청자 질문사항")
                        )
                ));

        verify(attendAdmissionFairUseCase, times(1)).execute(any(Long.class), any(AttendAdmissionFairRequest.class));
    }

    @Test
    void 입학설명회에_참가_신청을_할_때_해당_설명회가_없다면_에러가_발생한다() throws Exception {
        Long fairId = 1L;
        AttendAdmissionFairRequest request = FairFixture.createAttendAdmissionFairRequest();
        doThrow(new FairNotFoundException()).when(attendAdmissionFairUseCase).execute(any(Long.class), any(AttendAdmissionFairRequest.class));

        mockMvc.perform(post("/fairs/{fair-id}", fairId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isNotFound())

                .andDo(restDocs.document());

        verify(attendAdmissionFairUseCase, times(1)).execute(any(Long.class), any(AttendAdmissionFairRequest.class));
    }

    @Test
    void 입학설명회에_참가_신청을_할_때_인원_수를_초과했으면_에러가_발생한다() throws Exception {
        Long fairId = 1L;
        AttendAdmissionFairRequest request = FairFixture.createAttendAdmissionFairRequest();
        doThrow(new HeadcountExceededException()).when(attendAdmissionFairUseCase).execute(any(Long.class), any(AttendAdmissionFairRequest.class));

        mockMvc.perform(post("/fairs/{fair-id}", fairId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isConflict())

                .andDo(restDocs.document());

        verify(attendAdmissionFairUseCase, times(1)).execute(any(Long.class), any(AttendAdmissionFairRequest.class));
    }

    @Test
    void 입학설명회에_참가_신청을_할_때_신청_기간이_아니라면_에러가_발생한다() throws Exception {
        Long fairId = 1L;
        AttendAdmissionFairRequest request = FairFixture.createAttendAdmissionFairRequest();
        doThrow(new NotApplicationPeriodException()).when(attendAdmissionFairUseCase).execute(any(Long.class), any(AttendAdmissionFairRequest.class));

        mockMvc.perform(post("/fairs/{fair-id}", fairId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                )

                .andExpect(status().isConflict())

                .andDo(restDocs.document());

        verify(attendAdmissionFairUseCase, times(1)).execute(any(Long.class), any(AttendAdmissionFairRequest.class));
    }

    @Test
    void 입학설명회_일정을_불러온다() throws Exception {
        given(queryFairListUseCase.execute(any(FairType.class))).willReturn(FairFixture.createFairResponseList());

        mockMvc.perform(get("/fairs")
                        .param("type", FairType.STUDENT_AND_PARENT.name())
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isOk())

                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("type")
                                        .optional()
                                        .description("<<fair-type,입학설명회 유형 (미지정시 전체 조회)>>")
                        )
                ));

        verify(queryFairListUseCase, times(1)).execute(any(FairType.class));
    }

    @Test
    void 입학설명회를_상세히_불러온다() throws Exception {
        Long fairId = 1L;
        User user = UserFixture.createAdminUser();

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        given(queryFairDetailUseCase.execute(fairId, "none")).willReturn(FairFixture.createFairDetailResponse());

        mockMvc.perform(get("/fairs/{fair-id}", fairId)
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isOk())

                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer token")
                        ),
                        pathParameters(
                                parameterWithName("fair-id")
                                        .description("입학설명회 id")
                        ),
                        queryParameters(
                                parameterWithName("sort")
                                        .optional()
                                        .description("정렬 방식 (none, name_asc, name_desc)")
                        )
                ));

        verify(queryFairDetailUseCase, times(1)).execute(fairId, "none");
    }

    @Test
    void 입학설명회를_상세히_불러올_때_이름_오름차순으로_정렬한다() throws Exception {
        Long fairId = 1L;
        User user = UserFixture.createAdminUser();

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        given(queryFairDetailUseCase.execute(fairId, "name_asc")).willReturn(FairFixture.createFairDetailResponse());

        mockMvc.perform(get("/fairs/{fair-id}", fairId)
                        .param("sort", "name_asc")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isOk());

        verify(queryFairDetailUseCase, times(1)).execute(fairId, "name_asc");
    }

    @Test
    void 입학설명회를_상세히_불러올_때_이름_내림차순으로_정렬한다() throws Exception {
        Long fairId = 1L;
        User user = UserFixture.createAdminUser();

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        given(queryFairDetailUseCase.execute(fairId, "name_desc")).willReturn(FairFixture.createFairDetailResponse());

        mockMvc.perform(get("/fairs/{fair-id}", fairId)
                        .param("sort", "name_desc")
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isOk());

        verify(queryFairDetailUseCase, times(1)).execute(fairId, "name_desc");
    }

    @Test
    void 입학설명회를_상세히_불러올_때_해당_입학설명회가_없으면_에러가_발생한다() throws Exception {
        Long fairId = -1L;
        User user = UserFixture.createAdminUser();

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        willThrow(new FairNotFoundException()).given(queryFairDetailUseCase).execute(fairId, "none");

        mockMvc.perform(get("/fairs/{fair-id}", fairId)
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isNotFound())

                .andDo(restDocs.document());

        verify(queryFairDetailUseCase, times(1)).execute(fairId, "none");
    }

    @Test
    void 입학설명회_신청자_명단을_엑셀로_다운받는다() throws Exception {
        Long fairId = 1L;
        User user = UserFixture.createAdminUser();
        MockMultipartFile file = new MockMultipartFile(
                "입학설명회참가자명단",
                "입학설명회참가자명단.xlsx",
                String.valueOf(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                "<<file>>".getBytes()
        );

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        given(exportAttendeeListUseCase.execute(fairId)).willReturn(new ByteArrayResource(file.getBytes()));

        mockMvc.perform(get("/fairs/{fair-id}/export", fairId)
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))

                .andExpect(status().isOk())

                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer token")
                        ),
                        pathParameters(
                                parameterWithName("fair-id")
                                        .description("입학설명회 id")
                        )
                ));

        verify(exportAttendeeListUseCase, times(1)).execute(fairId);
    }

    @Test
    void 입학설명회_신청자_명단을_엑셀로_다운받을_때_입학설명회가_없으면_에러가_발생한다() throws Exception {
        Long fairId = -1L;
        User user = UserFixture.createAdminUser();

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        willThrow(new FairNotFoundException()).given(exportAttendeeListUseCase).execute(fairId);

        mockMvc.perform(get("/fairs/{fair-id}/export", fairId)
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader()))

                .andExpect(status().isNotFound())

                .andDo(restDocs.document());

        verify(exportAttendeeListUseCase, times(1)).execute(fairId);
    }

    @Test
    void 입학설명회_신청자를_삭제한다() throws Exception {
        Long fairId = 1L;
        Long attendeeId = 1L;
        User user = UserFixture.createAdminUser();

        DeleteAttendeeRequest deleteAttendeeRequest = new DeleteAttendeeRequest(attendeeId);
        DeleteAttendeeListRequest request = new DeleteAttendeeListRequest(List.of(deleteAttendeeRequest));

        given(authenticationArgumentResolver.supportsParameter(any(MethodParameter.class))).willReturn(true);
        given(authenticationArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
        willDoNothing().given(deleteAttendeeUseCase).execute(eq(fairId), any(DeleteAttendeeListRequest.class));

        mockMvc.perform(delete("/fairs/{fair-id}/attendees", fairId)
                        .header(HttpHeaders.AUTHORIZATION, AuthFixture.createAuthHeader())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))

                .andExpect(status().isNoContent())

                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                        .description("Bearer token")
                        ),
                        pathParameters(
                                parameterWithName("fair-id")
                                        .description("입학설명회 id")
                        ),
                        requestFields(
                                fieldWithPath("attendeeList")
                                        .type(JsonFieldType.ARRAY)
                                        .description("삭제할 신청자 목록"),
                                fieldWithPath("attendeeList[].attendeeId")
                                        .type(JsonFieldType.NUMBER)
                                        .description("신청자 id")
                        )
                ));

        verify(deleteAttendeeUseCase, times(1)).execute(eq(fairId), any(DeleteAttendeeListRequest.class));
    }
}