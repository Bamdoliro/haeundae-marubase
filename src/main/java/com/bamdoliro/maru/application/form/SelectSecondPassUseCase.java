package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.MissingTotalScoreException;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.constants.FixedNumber;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RequiredArgsConstructor
@UseCase
public class SelectSecondPassUseCase {

    /**
     * findFirstPassedSpecialForm의 정렬 순서를 그대로 옮긴 것. 마지막에 id 오름차순을 더해 순서를 고정한다.
     */
    private static final Comparator<Form> SECOND_ROUND_ORDER = Comparator
            .comparing((Form form) -> form.getScore().getSelfDirectedScore() + form.getScore().getPersonalityScore(), Comparator.reverseOrder())
            .thenComparing(form -> form.getScore().getSubjectGradeScore(), Comparator.reverseOrder())
            .thenComparing(form -> form.getScore().getThirdGradeSecondSemesterSubjectGradeScore(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(form -> form.getScore().getThirdGradeFirstSemesterSubjectGradeScore(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(form -> form.getScore().getSecondGradeSecondSemesterSubjectGradeScore(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(form -> form.getScore().getSecondGradeFirstSemesterSubjectGradeScore(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(form -> form.getApplicant().getBirthday())
            .thenComparing(Form::getId);

    private final FormRepository formRepository;
    private final CalculateFormScoreService calculateFormScoreService;

    @Transactional
    public void execute() {
        validate();

        int regularCount = FixedNumber.REGULAR;
        int socialIntegrationCount = FixedNumber.SPECIAL;
        int nationalVeteransEducationCount = FixedNumber.NATIONAL_VETERANS_EDUCATION;
        int specialAdmissionCount = FixedNumber.SPECIAL_ADMISSION;

        List<Form> specialFormList = formRepository.findFirstPassedSpecialForm();
        List<Form> equalOpportunityFormList = classifyFormsByType(specialFormList, FormType::isEqualOpportunity);
        List<Form> societyDiversityFormList = classifyFormsByType(specialFormList, FormType::isSocietyDiversity);

        if (specialFormList.size() < FixedNumber.SPECIAL) {
            int gap = FixedNumber.SPECIAL - specialFormList.size();
            regularCount += gap;
            socialIntegrationCount -= gap;
        }

        SocialIntegrationQuota quota = SocialIntegrationQuota.of(
                socialIntegrationCount,
                equalOpportunityFormList.size(),
                societyDiversityFormList.size()
        );

        // 요강: 기회균등전형 우선선발에서 탈락한 지원자는 사회다양성전형 대상자에 포함시켜 전형한다.
        // 사회다양성에서도 탈락해야 비로소 일반전형으로 전환된다.
        List<Form> equalOpportunityRejected = selectAndCollectRejected(equalOpportunityFormList, quota.equalOpportunityCount());
        List<Form> societyDiversityCandidates = Stream.concat(societyDiversityFormList.stream(), equalOpportunityRejected.stream())
                .sorted(SECOND_ROUND_ORDER)
                .toList();

        processForms(societyDiversityCandidates, quota.societyDiversityCount(), this::changeToRegularAndCalculateScoreAgain);

        formRepository.flush();
        List<Form> regularFormList = formRepository.findFirstPassedRegularForm();

        processForms(regularFormList, regularCount, Form::fail);

        formRepository.flush();
        List<Form> supernumeraryFormList = formRepository.findFirstPassedSupernumeraryForm();
        List<Form> nationalVeteransEducationFormList = classifyFormsByType(supernumeraryFormList, FormType::isNationalVeteransEducation);
        List<Form> specialAdmissionFormList = classifyFormsByType(supernumeraryFormList, FormType::isSpecialAdmission);

        processForms(nationalVeteransEducationFormList, nationalVeteransEducationCount, Form::fail);
        processForms(specialAdmissionFormList, specialAdmissionCount, Form::fail);
    }

    private void processForms(List<Form> formList, int count, Consumer<Form> action) {
        if (count <= 0) {
            formList.forEach(action);
            return;
        }

        for (Form form : formList) {
            if (count > 0) {
                form.pass();
                count--;
            } else {
                action.accept(form);
            }
        }
    }

    /**
     * processForms와 같은 규칙(정원 안에서 순서대로 합격, 동점자 규칙 없음)으로 합격 처리하고,
     * 탈락자에게 바로 action을 적용하는 대신 순서를 유지한 채 모아서 돌려준다.
     */
    private List<Form> selectAndCollectRejected(List<Form> formList, int count) {
        List<Form> rejected = new ArrayList<>();

        for (Form form : formList) {
            if (count > 0) {
                form.pass();
                count--;
            } else {
                rejected.add(form);
            }
        }
        return rejected;
    }

    private void changeToRegularAndCalculateScoreAgain(Form form) {
        form.changeToRegularSecondRound(calculateFormScoreService);
    }

    private void validate() {
        List<Form> firstPassedFormList = formRepository.findByStatus(FormStatus.FIRST_PASSED);
        firstPassedFormList.stream()
                .filter(form -> form.getScore().getTotalScore() == null)
                .forEach(form -> {
                    throw new MissingTotalScoreException();
                });
    }

    private List<Form> classifyFormsByType(List<Form> formList, FormTypeFilter filter) {
        return formList.stream()
                .filter(form -> filter.execute(form.getType()))
                .toList();
    }
}
