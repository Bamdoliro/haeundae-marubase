package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.response.FormSimpleResponse;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.util.RandomUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueryAllFormUseCaseTest {

    @InjectMocks
    private QueryAllFormUseCase queryAllFormUseCase;

    @Mock
    private FormRepository formRepository;

    @Test
    void 모든_원서를_조회한다() {
        // given
        List<Form> formList = List.of(
                FormFixture.createForm(FormType.REGULAR),
                FormFixture.createForm(FormType.SPECIAL_ADMISSION),
                FormFixture.createForm(FormType.NATIONAL_VETERANS),
                FormFixture.createForm(FormType.MULTI_CHILDREN)
        );

        formList.forEach(form -> form.assignExaminationNumber(1001L));

        given(formRepository.findByStatus(null)).willReturn(formList);

        // when
        List<FormSimpleResponse> returnedFormList = queryAllFormUseCase.execute(null, null, null);

        // then
        assertEquals(formList.size(), returnedFormList.size());

        verify(formRepository, times(1)).findByStatus(null);
    }

    @Test
    void 특별전형_원서만_조회한다() {
        // given
        List<Form> formList = List.of(
                FormFixture.createForm(FormType.REGULAR),
                FormFixture.createForm(FormType.SPECIAL_ADMISSION),
                FormFixture.createForm(FormType.NATIONAL_VETERANS),
                FormFixture.createForm(FormType.MULTI_CHILDREN)
        );

        formList.forEach(form -> form.assignExaminationNumber(1001L));

        given(formRepository.findByStatus(null)).willReturn(formList);

        // when
        List<FormSimpleResponse> returnedFormList = queryAllFormUseCase.execute(null, FormType.Category.SPECIAL, null);

        // then
        assertEquals(2, returnedFormList.size());

        verify(formRepository, times(1)).findByStatus(null);
    }

    @Test
    void 사회통합전형_원서만_조회한다() {
        // given
        List<Form> formList = List.of(
                FormFixture.createForm(FormType.REGULAR),
                FormFixture.createForm(FormType.SPECIAL_ADMISSION),
                FormFixture.createForm(FormType.NATIONAL_VETERANS),
                FormFixture.createForm(FormType.MULTI_CHILDREN)
        );

        formList.forEach(form -> form.assignExaminationNumber(1001L));

        given(formRepository.findByStatus(null)).willReturn(formList);

        // when
        List<FormSimpleResponse> returnedFormList = queryAllFormUseCase.execute(null, FormType.Category.SOCIETY_DIVERSITY, null);

        // then
        assertEquals(1, returnedFormList.size());

        verify(formRepository, times(1)).findByStatus(null);
    }

    @Test
    void 최종_점수가_높은_순으로_조회한다() {
        // given
        List<Form> formList = List.of(
                FormFixture.createForm(FormType.REGULAR),
                FormFixture.createForm(FormType.SPECIAL_ADMISSION),
                FormFixture.createForm(FormType.NATIONAL_VETERANS),
                FormFixture.createForm(FormType.MULTI_CHILDREN)
        );

        formList.stream()
                .filter(form -> form.getType() == FormType.NATIONAL_VETERANS)
                .forEach(form -> form.getScore().updateSecondRoundMeisterScore(
                        RandomUtil.randomDouble(10, 50),
                        RandomUtil.randomDouble(10, 50),
                        RandomUtil.randomDouble(10, 50)
                ));
        formList.stream()
                .filter(form -> form.getType() != FormType.NATIONAL_VETERANS)
                .forEach(form -> form.getScore().updateSecondRoundScore(
                        RandomUtil.randomDouble(10, 50),
                        RandomUtil.randomDouble(10, 50)
                ));
        given(formRepository.findByStatus(null)).willReturn(formList);

        // when
        List<FormSimpleResponse> returnedFormList = queryAllFormUseCase.execute(null, null, "total-score-desc");

        // then
        assertEquals(
                Collections.max(formList, Comparator.comparingDouble(form -> form.getScore().getTotalScore()))
                        .getScore()
                        .getTotalScore(),
                returnedFormList.get(0).getTotalScore()
        );

        verify(formRepository, times(1)).findByStatus(null);
    }

    @Test
    void 최종_점수가_낮은_순으로_조회한다() {
        // given
        List<Form> formList = List.of(
                FormFixture.createForm(FormType.REGULAR),
                FormFixture.createForm(FormType.SPECIAL_ADMISSION),
                FormFixture.createForm(FormType.NATIONAL_VETERANS),
                FormFixture.createForm(FormType.MULTI_CHILDREN)
        );

        formList.stream()
                .filter(form -> form.getType() == FormType.NATIONAL_VETERANS)
                .forEach(form -> form.getScore().updateSecondRoundMeisterScore(
                        RandomUtil.randomDouble(10, 50),
                        RandomUtil.randomDouble(10, 50),
                        RandomUtil.randomDouble(10, 50)
                ));
        formList.stream()
                .filter(form -> form.getType() != FormType.NATIONAL_VETERANS)
                .forEach(form -> form.getScore().updateSecondRoundScore(
                        RandomUtil.randomDouble(10, 50),
                        RandomUtil.randomDouble(10, 50)
                ));
        given(formRepository.findByStatus(null)).willReturn(formList);

        // when
        List<FormSimpleResponse> returnedFormList = queryAllFormUseCase.execute(null, null, "total-score-asc");

        // then
        assertEquals(
                Collections.min(formList, Comparator.comparingDouble(form -> form.getScore().getTotalScore()))
                        .getScore()
                        .getTotalScore(),
                returnedFormList.get(0).getTotalScore()
        );

        verify(formRepository, times(1)).findByStatus(null);
    }

    @Test
    void 접수번호가_높은_순으로_조회한다() {
        // given
        List<Form> formList = List.of(
                FormFixture.createForm(FormType.REGULAR),
                FormFixture.createForm(FormType.SPECIAL_ADMISSION),
                FormFixture.createForm(FormType.NATIONAL_VETERANS),
                FormFixture.createForm(FormType.MULTI_CHILDREN)
        );

        given(formRepository.findByStatus(null)).willReturn(formList);

        // when
        List<FormSimpleResponse> returnedFormList = queryAllFormUseCase.execute(null, FormType.Category.SOCIETY_DIVERSITY, "form-id");

        // then
        assertEquals(1, returnedFormList.size());

        verify(formRepository, times(1)).findByStatus(null);
    }
}