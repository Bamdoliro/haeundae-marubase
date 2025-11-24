package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.exception.FormAlreadySubmittedException;
import com.bamdoliro.maru.domain.form.service.AssignExaminationNumberService;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.request.SubmitFormRequest;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.annotation.ValidateApplicationFormPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class SubmitFormUseCase {

    private final FormRepository formRepository;
    private final CalculateFormScoreService calculateFormScoreService;
    private final AssignExaminationNumberService assignExaminationNumberService;

    @ValidateApplicationFormPeriod
    @Transactional
    public void execute(User user, SubmitFormRequest request) {
        Form form = formRepository.findByUser(user)
                .filter(existingForm -> existingForm.isRejected())
                .map(rejectedForm -> updateRejectedForm(rejectedForm, request))
                .orElseGet(() -> createNewForm(user, request));

        validateFormNotAlreadySubmitted(user);

        calculateFormScoreService.execute(form);
        assignExaminationNumberService.execute(form);
        formRepository.save(form);
    }

    private void validateFormNotAlreadySubmitted(User user) {
        formRepository.findByUser(user)
                .filter(existingForm -> !existingForm.isRejected())
                .ifPresent(form -> {
                    throw new FormAlreadySubmittedException();
                });
    }

    private Form updateRejectedForm(Form form, SubmitFormRequest request) {
        form.update(
                request.getApplicant().toValue(),
                request.getParent().toValue(),
                request.getEducation().toValue(),
                request.getGrade().toValue(),
                request.getDocument().toValue(),
                request.getType()
        );
        return form;
    }

    private Form createNewForm(User user, SubmitFormRequest request) {
        return Form.builder()
                .applicant(request.getApplicant().toValue())
                .parent(request.getParent().toValue())
                .education(request.getEducation().toValue())
                .grade(request.getGrade().toValue())
                .document(request.getDocument().toValue())
                .type(request.getType())
                .user(user)
                .build();
    }
}