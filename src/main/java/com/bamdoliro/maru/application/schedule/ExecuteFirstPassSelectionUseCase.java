package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.application.form.SelectFirstPassUseCase;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.domain.FirstPassSelectionExecution;
import com.bamdoliro.maru.domain.schedule.exception.FirstPassAlreadyExecutedException;
import com.bamdoliro.maru.domain.schedule.exception.FirstPassScheduleChangedException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.persistence.schedule.FirstPassSelectionExecutionRepository;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 일정의 예약 시각(executionTime) 하나에 대해 1차 합격자 선발을 한 번만 실행한다.
 * 같은 일정이라도 선발 시각을 바꿔 다시 예약하면 새 시각으로 다시 실행할 수 있다.
 * 실행 기록을 먼저 저장하고 같은 트랜잭션에서 선발을 돌리므로, 선발이 실패하면 기록도 함께 롤백된다.
 * 다른 서버가 같은 일정, 같은 시각의 기록을 먼저 저장했다면 unique 제약 위반(DataIntegrityViolationException)으로 실패한다.
 */
@RequiredArgsConstructor
@UseCase
public class ExecuteFirstPassSelectionUseCase {

    private final AdmissionScheduleFacade admissionScheduleFacade;
    private final FirstPassSelectionExecutionRepository executionRepository;
    private final SelectFirstPassUseCase selectFirstPassUseCase;

    @Transactional
    public void execute(Long scheduleId, LocalDateTime executionTime) {
        AdmissionSchedule schedule = admissionScheduleFacade.getSchedule(scheduleId);
        // 일정 수정 이벤트를 받지 못한 다른 서버에 남아 있는 옛 시각의 예약은 실행하지 않는다.
        if (!executionTime.equals(schedule.getSelectFirstPassAt())) {
            throw new FirstPassScheduleChangedException();
        }
        if (executionRepository.existsByScheduleAndExecutionTime(schedule, executionTime)) {
            throw new FirstPassAlreadyExecutedException();
        }

        executionRepository.saveAndFlush(new FirstPassSelectionExecution(schedule, executionTime));
        selectFirstPassUseCase.execute();
    }
}
