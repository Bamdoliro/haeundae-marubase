package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.value.Subject;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@UseCase
public class SubmitFormUseCase {

    private final FormRepository formRepository;
    private final CalculateFormScoreService calculateFormScoreService;
    private final AssignExaminationNumberService assignExaminationNumberService;

    @ValidateApplicationFormPeriod
    @Transactional
    public void execute(User user, SubmitFormRequest request) {
        validateOnlyOneFormPerUser(user);

        Form form = Form.builder()
                .applicant(request.getApplicant().toValue())
                .parent(request.getParent().toValue())
                .education(request.getEducation().toValue())
                .grade(request.getGrade().toValue())
                .document(request.getDocument().toValue())
                .type(request.getType())
                .user(user)
                .build();

        if (!form.getEducation().isQualificationExamination()) {
            List<Subject> subjectList = form.getGrade().getSubjectList().getSubjectMap().getValue().get("21");
            for (Subject subject : subjectList) {
                if (Objects.equals(subject.getSubjectName(), "수학")) {
                    System.out.println(subject.getScore());
                }
            }
        }


        calculateFormScoreService.execute(form);
        assignExaminationNumberService.execute(form);
        formRepository.save(form);
    }

    private void validateOnlyOneFormPerUser(User user) {
        Optional<Form> form = formRepository.findByUser(user);
        if (form.isPresent()) {
            throw new FormAlreadySubmittedException();
        }
    }
}
