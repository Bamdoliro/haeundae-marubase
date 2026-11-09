package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.constants.FixedNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RequiredArgsConstructor
@UseCase
public class SelectFirstPassUseCase {

    /**
     * findApprovedSpecialForm과 같은 순서: 1차 점수 내림차순, 동점이면 id 오름차순.
     */
    private static final Comparator<Form> FIRST_ROUND_ORDER = Comparator
            .comparing((Form form) -> form.getScore().getFirstRoundScore(), Comparator.reverseOrder())
            .thenComparing(Form::getId);

    private final FormRepository formRepository;
    private final CalculateFormScoreService calculateFormScoreService;

    @Transactional
    public void execute() {
        int regularCount = FixedNumber.REGULAR;
        int socialIntegrationCount = FixedNumber.SPECIAL;
        int nationalVeteransEducationCount = FixedNumber.NATIONAL_VETERANS_EDUCATION;
        int specialAdmissionCount = FixedNumber.SPECIAL_ADMISSION;

        List<Form> specialFormList = formRepository.findApprovedSpecialForm()
                .stream()
                .filter(Form::isPaid)
                .toList();
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
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(
                socialIntegrationCount,
                equalOpportunityFormList.size(),
                societyDiversityFormList.size()
        );

        // 요강: 기회균등전형 우선선발에서 탈락한 지원자는 사회다양성전형 대상자에 포함시켜 전형한다.
        // 사회다양성에서도 탈락해야 비로소 일반전형으로 전환된다.
        List<Form> equalOpportunityRejected = selectAndCollectRejected(equalOpportunityFormList, quota.equalOpportunityCount());
        List<Form> societyDiversityCandidates = Stream.concat(societyDiversityFormList.stream(), equalOpportunityRejected.stream())
                .sorted(FIRST_ROUND_ORDER)
                .toList();

        processForms(societyDiversityCandidates, quota.societyDiversityCount(), this::changeToRegularAndCalculateGradeAgain);

        formRepository.flush();
        List<Form> regularFormList = formRepository.findApprovedRegularForm()
                .stream()
                .filter(Form::isPaid)
                .toList();

        processForms(regularFormList, regularCount, Form::firstFail);

        formRepository.flush();
        List<Form> supernumeraryFormList = formRepository.findApprovedSupernumeraryForm()
                .stream()
                .filter(Form::isPaid)
                .toList();
        List<Form> nationalVeteransFormList = classifyFormsByType(supernumeraryFormList, FormType::isNationalVeteransEducation);
        List<Form> specialAdmissionFormList = classifyFormsByType(supernumeraryFormList, FormType::isSpecialAdmission);

        processForms(nationalVeteransFormList, nationalVeteransEducationCount, Form::firstFail);
        processForms(specialAdmissionFormList, specialAdmissionCount, Form::firstFail);
    }

    private void processForms(List<Form> formList, int count, Consumer<Form> action) {
        if (formList.isEmpty())
            return;

        if (count <= 0) {
            formList.forEach(action);
            return;
        }

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

    /**
     * processForms와 같은 규칙(정원 안 + 마지막 합격자와 동점자 전원 합격)으로 1차 합격 처리하고,
     * 탈락자에게 바로 action을 적용하는 대신 순서를 유지한 채 모아서 돌려준다.
     */
    private List<Form> selectAndCollectRejected(List<Form> formList, int count) {
        if (formList.isEmpty()) {
            return List.of();
        }
        if (count <= 0) {
            return new ArrayList<>(formList);
        }

        Double lastScore = formList.get(Math.min(count, formList.size()) - 1).getScore().getFirstRoundScore();
        List<Form> rejected = new ArrayList<>();

        for (Form form : formList) {
            if (count > 0) {
                form.firstPass();
                count--;
            } else if (form.getScore().getFirstRoundScore().equals(lastScore)) {
                form.firstPass();
            } else {
                rejected.add(form);
            }
        }
        return rejected;
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