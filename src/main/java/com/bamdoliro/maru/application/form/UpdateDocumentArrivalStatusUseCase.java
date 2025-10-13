package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.request.DocumentArrivalStatusListRequest;
import com.bamdoliro.maru.presentation.form.dto.request.DocumentArrivalStatusRequest;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@UseCase
public class UpdateDocumentArrivalStatusUseCase {

    private final FormRepository formRepository;

    @Transactional
    public void execute(DocumentArrivalStatusListRequest request) {
        List<DocumentArrivalStatusRequest> requestList = getSortedList(request);

        List<Form> formList = formRepository.findByFormIdList(
                requestList.stream()
                        .map(DocumentArrivalStatusRequest::getFormId)
                        .toList()
        );

        validate(requestList, formList);

        for (int i = 0; i < formList.size(); i++) {
            Form form = formList.get(i);
            boolean arrived = requestList.get(i).isArrived();

            if (arrived) {
                form.arrive();
            } else {
                form.notArrive();
            }
        }
    }

    private List<DocumentArrivalStatusRequest> getSortedList(DocumentArrivalStatusListRequest request) {
        return request.getFormList().stream()
                .sorted(Comparator.comparingLong(DocumentArrivalStatusRequest::getFormId))
                .toList();
    }

    private void validate(List<DocumentArrivalStatusRequest> requestList, List<Form> formList) {
        if (requestList.size() != formList.size()) {
            throw new FormNotFoundException();
        }
    }
}