package com.bamdoliro.maru.infrastructure.aop.schedule;

import com.bamdoliro.maru.domain.form.exception.OutOfApplicationFormPeriodException;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 원서 접수 기간은 DB의 활성 입학 일정(applicationFormStart ~ applicationFormEnd)으로 판단한다.
 */
@RequiredArgsConstructor
@Aspect
@Component
public class ScheduleValidationAspect {

    private final AdmissionScheduleFacade admissionScheduleFacade;
    private final Clock serviceClock;

    @Before("@annotation(com.bamdoliro.maru.shared.annotation.ValidateApplicationFormPeriod)")
    public void validateApplicationFormPeriod() {
        AdmissionSchedule schedule = admissionScheduleFacade.getCurrentSchedule();
        LocalDateTime now = LocalDateTime.now(serviceClock);
        if (now.isBefore(schedule.getApplicationFormStart())
                || now.isAfter(schedule.getApplicationFormEnd())) {
            throw new OutOfApplicationFormPeriodException();
        }
    }
}
