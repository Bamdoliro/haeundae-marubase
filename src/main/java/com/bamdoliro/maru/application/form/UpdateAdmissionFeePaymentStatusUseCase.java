package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.request.AdmissionFeePaymentStatusListRequest;
import com.bamdoliro.maru.presentation.form.dto.request.AdmissionFeePaymentStatusRequest;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@UseCase
public class UpdateAdmissionFeePaymentStatusUseCase {

    private final FormRepository formRepository;

    @Transactional
    public void execute(AdmissionFeePaymentStatusListRequest request) {
        List<AdmissionFeePaymentStatusRequest> requestList = getSortedList(request);

        List<Form> formList = formRepository.findByFormIdList(
                requestList.stream()
                        .map(AdmissionFeePaymentStatusRequest::getFormId)
                        .toList()
        );

        validate(requestList, formList);

        for (int i = 0; i < formList.size(); i++) {
            Form form = formList.get(i);
            boolean paid = requestList.get(i).isPaid();

            if (paid) {
                form.payFee();
            } else {
                form.cancelPayment();
            }
        }
    }

    private List<AdmissionFeePaymentStatusRequest> getSortedList(AdmissionFeePaymentStatusListRequest request) {
        return request.getFormList().stream()
                .sorted(Comparator.comparingLong(AdmissionFeePaymentStatusRequest::getFormId))
                .toList();
    }

    private void validate(List<AdmissionFeePaymentStatusRequest> requestList, List<Form> formList) {
        if (requestList.size() != formList.size()) {
            throw new FormNotFoundException();
        }
    }
}