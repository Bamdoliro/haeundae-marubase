package com.bamdoliro.maru.presentation.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleNotFoundException;
import com.bamdoliro.maru.presentation.schedule.dto.response.ScheduleResponse;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.bamdoliro.maru.shared.util.RestDocsTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScheduleControllerTest extends RestDocsTestSupport {

    static ScheduleResponse createResponse() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        ReflectionTestUtils.setField(schedule, "id", 1L);
        ReflectionTestUtils.setField(schedule, "version", 0L);
        return new ScheduleResponse(schedule);
    }

    @Test
    void 현재_입학_일정을_조회한다() throws Exception {
        given(queryCurrentScheduleUseCase.execute()).willReturn(createResponse());

        mockMvc.perform(get("/schedules/current")
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admissionYear").value(2027))
                .andExpect(jsonPath("$.data.applicationFormStart").value("2026-10-20T09:00:00"))
                .andExpect(jsonPath("$.data.visitSubmissionAt").value("2026-10-27T09:00:00"))
                .andExpect(jsonPath("$.data.attendanceStandardDate").value("2026-10-01"))

                .andDo(restDocs.document());
    }

    @Test
    void 등록된_입학_일정이_없으면_404를_반환한다() throws Exception {
        given(queryCurrentScheduleUseCase.execute()).willThrow(new ScheduleNotFoundException());

        mockMvc.perform(get("/schedules/current")
                        .accept(MediaType.APPLICATION_JSON)
                )

                .andExpect(status().isNotFound())

                .andDo(restDocs.document());
    }
}
