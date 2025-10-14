package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class UpdateExaminationNumberUseCase {

    private final FormFacade formFacade;

    @Transactional
    public void execute(Long formId, Long examinationNumber) {
        Form form = formFacade.getForm(formId);
        form.assignExaminationNumber(examinationNumber);
    }
}