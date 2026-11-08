package com.bamdoliro.maru.domain.schedule;

import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.exception.InvalidSchedulePeriodException;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.UnaryOperator;

import static com.bamdoliro.maru.shared.fixture.ScheduleFixture.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdmissionScheduleTest {

    private static AdmissionSchedule.AdmissionScheduleBuilder validBuilder() {
        return AdmissionSchedule.builder()
                .admissionYear(2027)
                .active(true)
                .applicationFormStart(APPLICATION_FORM_START)
                .visitSubmissionAt(VISIT_SUBMISSION_AT)
                .applicationFormEnd(APPLICATION_FORM_END)
                .announcementOfFirstPass(ANNOUNCEMENT_OF_FIRST_PASS)
                .interviewStart(INTERVIEW_START)
                .interviewEnd(INTERVIEW_END)
                .announcementOfFinalPass(ANNOUNCEMENT_OF_FINAL_PASS)
                .registrationStart(REGISTRATION_START)
                .registrationEnd(REGISTRATION_END)
                .attendanceStandardDate(ATTENDANCE_STANDARD_DATE);
    }

    private static void assertInvalid(UnaryOperator<AdmissionSchedule.AdmissionScheduleBuilder> modifier) {
        assertThrows(InvalidSchedulePeriodException.class, () -> modifier.apply(validBuilder()).build());
    }

    @Test
    void 시간_순서가_올바르면_일정을_만든다() {
        AdmissionSchedule schedule = assertDoesNotThrow(() -> validBuilder().build());
        assertEquals(2027, schedule.getAdmissionYear());
    }

    @Test
    void 방문_원서_접수가_원서_접수_시작과_같아도_된다() {
        assertDoesNotThrow(() -> validBuilder().visitSubmissionAt(APPLICATION_FORM_START).build());
    }

    @Test
    void 출결_기준일이_원서_접수_마감일과_같아도_된다() {
        assertDoesNotThrow(() -> validBuilder().attendanceStandardDate(APPLICATION_FORM_END.toLocalDate()).build());
    }

    @Test
    void 필수값이_비어_있으면_에러가_발생한다() {
        assertInvalid(builder -> builder.admissionYear(null));
        assertInvalid(builder -> builder.interviewEnd(null));
        assertInvalid(builder -> builder.attendanceStandardDate(null));
    }

    @Test
    void 원서_접수_마감이_시작보다_빠르면_에러가_발생한다() {
        assertInvalid(builder -> builder.applicationFormEnd(APPLICATION_FORM_START.minusDays(1)));
    }

    @Test
    void 방문_원서_접수가_접수_기간_밖이면_에러가_발생한다() {
        assertInvalid(builder -> builder.visitSubmissionAt(APPLICATION_FORM_START.minusMinutes(1)));
        assertInvalid(builder -> builder.visitSubmissionAt(APPLICATION_FORM_END));
    }

    @Test
    void 일차_합격_발표가_원서_접수_마감보다_빠르면_에러가_발생한다() {
        assertInvalid(builder -> builder.announcementOfFirstPass(APPLICATION_FORM_END.minusHours(1)));
    }

    @Test
    void 면접_일정이_발표_순서와_맞지_않으면_에러가_발생한다() {
        assertInvalid(builder -> builder.interviewStart(ANNOUNCEMENT_OF_FIRST_PASS.minusHours(1)));
        assertInvalid(builder -> builder.interviewEnd(INTERVIEW_START));
        assertInvalid(builder -> builder.announcementOfFinalPass(INTERVIEW_END.minusMinutes(1)));
    }

    @Test
    void 입학_등록_기간이_올바르지_않으면_에러가_발생한다() {
        assertInvalid(builder -> builder.registrationStart(ANNOUNCEMENT_OF_FINAL_PASS));
        assertInvalid(builder -> builder.registrationEnd(REGISTRATION_START.minusHours(1)));
    }

    @Test
    void 출결_기준일이_원서_접수_마감일보다_늦으면_에러가_발생한다() {
        assertInvalid(builder -> builder.attendanceStandardDate(APPLICATION_FORM_END.toLocalDate().plusDays(1)));
    }

    @Test
    void 수정할_때도_시간_순서를_검증하고_실패하면_기존_값을_유지한다() {
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        LocalDateTime wrongEnd = APPLICATION_FORM_START.minusDays(1);

        assertThrows(InvalidSchedulePeriodException.class, () -> schedule.update(
                2027,
                APPLICATION_FORM_START,
                VISIT_SUBMISSION_AT,
                wrongEnd,
                ANNOUNCEMENT_OF_FIRST_PASS,
                INTERVIEW_START,
                INTERVIEW_END,
                ANNOUNCEMENT_OF_FINAL_PASS,
                REGISTRATION_START,
                REGISTRATION_END,
                LocalDate.of(2026, 10, 1)
        ));
        assertEquals(APPLICATION_FORM_END, schedule.getApplicationFormEnd());
    }
}
