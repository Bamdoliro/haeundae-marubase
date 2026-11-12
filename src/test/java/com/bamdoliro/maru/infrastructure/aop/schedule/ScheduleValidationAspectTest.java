package com.bamdoliro.maru.infrastructure.aop.schedule;

import com.bamdoliro.maru.domain.form.exception.OutOfApplicationFormPeriodException;
import com.bamdoliro.maru.domain.schedule.exception.ScheduleNotFoundException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.shared.config.TimeConfig;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ScheduleValidationAspectTest {

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    private ScheduleValidationAspect aspectAt(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(TimeConfig.SERVICE_ZONE_ID).toInstant(), TimeConfig.SERVICE_ZONE_ID);
        return new ScheduleValidationAspect(admissionScheduleFacade, clock);
    }

    @Test
    void 활성_일정의_원서_접수_기간_안이면_통과한다() {
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));

        assertDoesNotThrow(() -> aspectAt(ScheduleFixture.APPLICATION_FORM_START.plusSeconds(1))
                .validateApplicationFormPeriod());
        assertDoesNotThrow(() -> aspectAt(ScheduleFixture.APPLICATION_FORM_END)
                .validateApplicationFormPeriod());
    }

    @Test
    void 원서_접수_시작_전이면_에러가_발생한다() {
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));

        assertThrows(OutOfApplicationFormPeriodException.class,
                () -> aspectAt(ScheduleFixture.APPLICATION_FORM_START.minusSeconds(1)).validateApplicationFormPeriod());
    }

    @Test
    void 원서_접수_마감_후면_에러가_발생한다() {
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));

        assertThrows(OutOfApplicationFormPeriodException.class,
                () -> aspectAt(ScheduleFixture.APPLICATION_FORM_END.plusSeconds(1)).validateApplicationFormPeriod());
    }

    @Test
    void 활성_일정이_없으면_에러가_발생한다() {
        given(admissionScheduleFacade.getCurrentSchedule()).willThrow(new ScheduleNotFoundException());

        assertThrows(ScheduleNotFoundException.class,
                () -> aspectAt(ScheduleFixture.APPLICATION_FORM_START).validateApplicationFormPeriod());
    }
}
