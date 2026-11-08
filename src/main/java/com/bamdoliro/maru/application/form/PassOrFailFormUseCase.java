package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.domain.form.exception.InvalidFormResultRequestException;
import com.bamdoliro.maru.domain.form.exception.InvalidFormStatusException;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.request.PassOrFailFormListRequest;
import com.bamdoliro.maru.presentation.form.dto.request.PassOrFailFormRequest;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@UseCase
public class PassOrFailFormUseCase {

    /**
     * 수동 합불 처리는 1차 합격 이후 단계의 원서에만 허용한다.
     * 이미 PASSED/FAILED인 원서는 결과를 정정할 수 있도록 다시 처리할 수 있다.
     */
    private static final Set<FormStatus> ALLOWED_STATUS = Set.of(
            FormStatus.FIRST_PASSED, FormStatus.PASSED, FormStatus.FAILED
    );

    private final FormRepository formRepository;

    @Transactional
    public void execute(PassOrFailFormListRequest request) {
        validateRequest(request.getFormList());
        List<PassOrFailFormRequest> requestList = getSortedList(request);

        // 다른 관리자의 동시 처리와 겹치지 않도록 검증 전에 원서 행을 먼저 잠근다.
        List<Form> formList = formRepository.findByFormIdListForUpdate(
                requestList.stream()
                        .map(PassOrFailFormRequest::getFormId)
                        .toList()
        );

        validateAllFound(requestList, formList);
        validateStatus(formList);

        for (int i = 0; i < formList.size(); i++) {
            Form form = formList.get(i);
            boolean pass = requestList.get(i).isPass();

            if (pass) {
                form.pass();
            } else {
                form.fail();
            }
        }
    }

    private void validateRequest(List<PassOrFailFormRequest> requestList) {
        if (requestList.stream().anyMatch(r -> r == null || r.getFormId() == null)) {
            throw new InvalidFormResultRequestException("원서 번호가 비어 있는 항목이 있습니다.");
        }

        long distinctCount = requestList.stream()
                .map(PassOrFailFormRequest::getFormId)
                .distinct()
                .count();
        if (distinctCount != requestList.size()) {
            throw new InvalidFormResultRequestException("같은 원서가 두 번 이상 포함되어 있습니다.");
        }
    }

    private List<PassOrFailFormRequest> getSortedList(PassOrFailFormListRequest request) {
        return request.getFormList().stream()
                .sorted(Comparator.comparingLong(PassOrFailFormRequest::getFormId))
                .toList();
    }

    private void validateAllFound(List<PassOrFailFormRequest> requestList, List<Form> formList) {
        if (requestList.size() != formList.size()) {
            throw new FormNotFoundException();
        }
    }

    private void validateStatus(List<Form> formList) {
        boolean hasInvalidStatus = formList.stream()
                .map(Form::getStatus)
                .anyMatch(status -> !ALLOWED_STATUS.contains(status));
        if (hasInvalidStatus) {
            throw new InvalidFormStatusException();
        }
    }
}
