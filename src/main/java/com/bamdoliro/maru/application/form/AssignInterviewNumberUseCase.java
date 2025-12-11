package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.request.AssignInterviewNumberListRequest;
import com.bamdoliro.maru.presentation.form.dto.request.AssignInterviewNumberRequest;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@UseCase
public class AssignInterviewNumberUseCase {

    private final FormRepository formRepository;

    @Transactional
    public void execute(AssignInterviewNumberListRequest request) {
        List<AssignInterviewNumberRequest> requestList = getSortedList(request);

        List<Form> formList = formRepository.findByFormIdList(
                requestList.stream()
                        .map(AssignInterviewNumberRequest::getFormId)
                        .toList()
        );

        validate(requestList, formList);

        for (int i = 0; i < formList.size(); i++) {
            Form form = formList.get(i);
            String interviewNumber = requestList.get(i).getInterviewNumber();

            if (interviewNumber == null) {
                continue;
            }

            form.assignInterviewNumber(interviewNumber);
        }

    }

    private List<AssignInterviewNumberRequest> getSortedList(AssignInterviewNumberListRequest request) {
        return request.getFormList().stream()
                .sorted(Comparator.comparingLong(AssignInterviewNumberRequest::getFormId))
                .toList();
    }

    private void validate(List<AssignInterviewNumberRequest> requestList, List<Form> formList) {
        if (requestList.size() != formList.size()) {
            throw new FormNotFoundException();
        }
    }
}