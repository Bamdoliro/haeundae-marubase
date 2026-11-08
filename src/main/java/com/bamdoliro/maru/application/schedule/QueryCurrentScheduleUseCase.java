package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.presentation.schedule.dto.response.ScheduleResponse;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 활성 일정을 조회한다. 프론트엔드가 환경변수로 읽던 전형 일정을 대체한다.
 * 등록된 일정이 없으면 404(ScheduleNotFoundException).
 */
@RequiredArgsConstructor
@UseCase
public class QueryCurrentScheduleUseCase {

    private final AdmissionScheduleFacade admissionScheduleFacade;

    @Transactional(readOnly = true)
    public ScheduleResponse execute() {
        return new ScheduleResponse(admissionScheduleFacade.getCurrentSchedule());
    }
}
