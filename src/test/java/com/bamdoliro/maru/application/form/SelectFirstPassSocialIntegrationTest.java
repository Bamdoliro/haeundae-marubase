package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.value.Score;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SelectFirstPassSocialIntegrationTest {

    private FormRepository formRepository;
    private SelectFirstPassUseCase selectFirstPassUseCase;

    @BeforeEach
    void setUp() {
        formRepository = mock(FormRepository.class);
        selectFirstPassUseCase = new SelectFirstPassUseCase(formRepository, new CalculateFormScoreService());

        given(formRepository.findApprovedRegularForm()).willReturn(List.of());
        given(formRepository.findApprovedSupernumeraryForm()).willReturn(List.of());
    }

    @Test
    void 기회균등이_미달이면_사회다양성이_더_선발된다() {
        // 사회통합 정원 36 x 1.5 = 54 -> 기본 27 / 27
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 10);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 50);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectFirstPassUseCase.execute();

        assertEquals(10, countFirstPassed(equalOpportunity));
        assertEquals(44, countFirstPassed(societyDiversity));
    }

    @Test
    void 사회다양성이_미달이면_기회균등이_더_선발된다() {
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 50);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 10);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectFirstPassUseCase.execute();

        assertEquals(44, countFirstPassed(equalOpportunity));
        assertEquals(10, countFirstPassed(societyDiversity));
    }

    @Test
    void 양쪽_모두_정원_이상이면_반반으로_선발된다() {
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 40);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 40);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectFirstPassUseCase.execute();

        assertEquals(27, countFirstPassed(equalOpportunity));
        assertEquals(27, countFirstPassed(societyDiversity));
    }

    @Test
    void 사회통합_지원자가_없어_정원이_0이어도_정상_처리된다() {
        given(formRepository.findApprovedSpecialForm()).willReturn(List.of());

        assertDoesNotThrow(() -> selectFirstPassUseCase.execute());
    }

    @Test
    void 선발_인원이_0이면_전원에게_탈락_처리를_적용한다() {
        List<Form> formList = createForms(FormType.REGULAR, 3);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                selectFirstPassUseCase, "processForms", formList, 0, (Consumer<Form>) Form::firstFail
        ));
        assertTrue(formList.stream().allMatch(form -> form.getStatus() == FormStatus.FIRST_FAILED));
    }

    private void givenSpecialForms(List<Form> equalOpportunity, List<Form> societyDiversity) {
        List<Form> special = new ArrayList<>(equalOpportunity);
        special.addAll(societyDiversity);
        given(formRepository.findApprovedSpecialForm()).willReturn(special);
    }

    private List<Form> createForms(FormType type, int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> {
                    Form form = FormFixture.createForm(type);
                    form.approve();
                    form.payFee();
                    form.updateScore(new Score(130.0 - i * 0.5, 0));
                    return form;
                })
                .toList();
    }

    private long countFirstPassed(List<Form> formList) {
        return formList.stream()
                .filter(form -> form.getStatus() == FormStatus.FIRST_PASSED)
                .count();
    }
}
