package com.bamdoliro.maru.application.schedule;

import com.bamdoliro.maru.application.form.SelectFirstPassUseCase;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.domain.FirstPassSelectionExecution;
import com.bamdoliro.maru.domain.schedule.exception.FirstPassAlreadyExecutedException;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.persistence.schedule.FirstPassSelectionExecutionRepository;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일정 하나에 대해 1차 합격자 선발을 한 번만 실행한다.
 * 실행 기록을 먼저 저장하고 같은 트랜잭션에서 선발을 돌리므로, 선발이 실패하면 기록도 함께 롤백된다.
 * 다른 서버가 먼저 기록을 저장했다면 unique 제약 위반(DataIntegrityViolationException)으로 실패한다.
 */
@RequiredArgsConstructor
@UseCase
public class ExecuteFirstPassSelectionUseCase {

    private final AdmissionScheduleFacade admissionScheduleFacade;
    private final FirstPassSelectionExecutionRepository executionRepository;
    private final SelectFirstPassUseCase selectFirstPassUseCase;

    @Transactional
    public void execute(Long scheduleId) {
        AdmissionSchedule schedule = admissionScheduleFacade.getSchedule(scheduleId);
        if (executionRepository.existsBySchedule(schedule)) {
            throw new FirstPassAlreadyExecutedException();
        }

        executionRepository.saveAndFlush(new FirstPassSelectionExecution(schedule));
        selectFirstPassUseCase.execute();
    }
}
