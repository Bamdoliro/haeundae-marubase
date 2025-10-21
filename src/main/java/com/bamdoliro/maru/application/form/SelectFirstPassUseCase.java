package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.constants.FixedNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
@UseCase
public class SelectFirstPassUseCase {

    private final FormRepository formRepository;
    private final CalculateFormScoreService calculateFormScoreService;

    @Transactional
    public void execute() {
        int regularCount = FixedNumber.REGULAR;
        int socialIntegrationCount = FixedNumber.SPECIAL;
        int nationalVeteransEducationCount = FixedNumber.NATIONAL_VETERANS_EDUCATION;
        int specialAdmissionCount = FixedNumber.SPECIAL_ADMISSION;

        List<Form> specialFormList = formRepository.findReceivedSpecialForm();
        List<Form> equalOpportunityFormList = classifyFormsByType(specialFormList, FormType::isEqualOpportunity);
        List<Form> societyDiversityFormList = classifyFormsByType(specialFormList, FormType::isSocietyDiversity);

        if (specialFormList.size() < FixedNumber.SPECIAL) {
            int gap = FixedNumber.SPECIAL - specialFormList.size();
            regularCount += gap;
            socialIntegrationCount -= gap;
        }

        regularCount = calculateMultiple(regularCount);
        socialIntegrationCount = calculateMultiple(socialIntegrationCount);
        nationalVeteransEducationCount = calculateMultiple(nationalVeteransEducationCount);
        specialAdmissionCount = calculateMultiple(specialAdmissionCount);
        int equalOpportunityCount = (int) Math.round(socialIntegrationCount * 0.5);
        int societyDiversityCount = equalOpportunityCount;

        processForms(equalOpportunityFormList, equalOpportunityCount, this::changeToRegularAndCalculateGradeAgain);
        processForms(societyDiversityFormList, societyDiversityCount, this::changeToRegularAndCalculateGradeAgain);

        formRepository.flush();
        List<Form> regularFormList = formRepository.findReceivedRegularForm();

        processForms(regularFormList, regularCount, Form::firstFail);

        formRepository.flush();
        List<Form> supernumeraryFormList = formRepository.findReceivedSupernumeraryForm();
        List<Form> nationalVeteransFormList = classifyFormsByType(supernumeraryFormList, FormType::isNationalVeteransEducation);
        List<Form> specialAdmissionFormList = classifyFormsByType(supernumeraryFormList, FormType::isSpecialAdmission);

        processForms(nationalVeteransFormList, nationalVeteransEducationCount, Form::firstFail);
        processForms(specialAdmissionFormList, specialAdmissionCount, Form::firstFail);
    }

    private void processForms(List<Form> formList, int count, Consumer<Form> action) {
        if (formList.isEmpty())
            return;

        Double lastScore = formList.get(Math.min(count, formList.size()) - 1).getScore().getFirstRoundScore();

        for (Form form : formList) {
            if (count > 0) {
                form.firstPass();
                count--;
            } else if (form.getScore().getFirstRoundScore().equals(lastScore)) {
                form.firstPass();
            } else {
                action.accept(form);
            }
        }
    }

    private void changeToRegularAndCalculateGradeAgain(Form form) {
        form.changeToRegularFirstRound(calculateFormScoreService);
    }

    private List<Form> classifyFormsByType(List<Form> formList, FormTypeFilter filter) {
        return formList.stream()
                .filter(form -> filter.execute(form.getType()))
                .toList();
    }

    private int calculateMultiple(int count) {
        return (int) Math.ceil(count * FixedNumber.MULTIPLE);
    }
}

@FunctionalInterface
interface FormTypeFilter {
    boolean execute(FormType type);
}