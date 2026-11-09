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

class SelectSecondPassSocialIntegrationTest {

    private FormRepository formRepository;
    private SelectSecondPassUseCase selectSecondPassUseCase;
    private long nextId = 1;

    @BeforeEach
    void setUp() {
        formRepository = mock(FormRepository.class);
        selectSecondPassUseCase = new SelectSecondPassUseCase(formRepository, new CalculateFormScoreService());

        given(formRepository.findByStatus(FormStatus.FIRST_PASSED)).willReturn(List.of());
        given(formRepository.findFirstPassedRegularForm()).willReturn(List.of());
        given(formRepository.findFirstPassedSupernumeraryForm()).willReturn(List.of());
    }

    @Test
    void 기회균등이_미달이면_사회다양성이_더_선발된다() {
        // 사회통합 정원 36 (배수 없음) -> 기본 18 / 18
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 5);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 40);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectSecondPassUseCase.execute();

        assertEquals(5, countPassed(equalOpportunity));
        assertEquals(31, countPassed(societyDiversity));
    }

    @Test
    void 사회다양성이_미달이면_기회균등이_더_선발된다() {
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 40);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 5);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectSecondPassUseCase.execute();

        assertEquals(31, countPassed(equalOpportunity));
        assertEquals(5, countPassed(societyDiversity));
    }

    @Test
    void 양쪽_모두_정원_이상이면_반반으로_선발된다() {
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 30);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 30);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectSecondPassUseCase.execute();

        assertEquals(18, countPassed(equalOpportunity));
        assertEquals(18, countPassed(societyDiversity));
    }

    @Test
    void 사회통합_지원자가_없어_정원이_0이어도_정상_처리된다() {
        given(formRepository.findFirstPassedSpecialForm()).willReturn(List.of());

        assertDoesNotThrow(() -> selectSecondPassUseCase.execute());
    }

    @Test
    void 선발_인원이_0이면_전원에게_불합격_처리를_적용한다() {
        List<Form> formList = createForms(FormType.REGULAR, 3);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                selectSecondPassUseCase, "processForms", formList, 0, (Consumer<Form>) Form::fail
        ));
        assertTrue(formList.stream().allMatch(form -> form.getStatus() == FormStatus.FAILED));
    }

    @Test
    void 기회균등_탈락자도_순위가_높으면_사회다양성_정원으로_합격한다() {
        // 2차 순위는 자기주도학습+인성 점수 합이 먼저다. 기회균등 18명 우선선발, 탈락 12명이 사회다양성 30명과 18자리를 놓고 경쟁
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 30, 60.0);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 30, 40.0);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectSecondPassUseCase.execute();

        assertEquals(30, countPassed(equalOpportunity));
        assertEquals(6, countPassed(societyDiversity));
        assertEquals(0, countChangedToRegular(equalOpportunity));
        assertEquals(24, countChangedToRegular(societyDiversity));
    }

    @Test
    void 기회균등_탈락자가_사회다양성_정원에서도_밀리면_일반전형으로_전환된다() {
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 30, 40.0);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 30, 60.0);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectSecondPassUseCase.execute();

        assertEquals(18, countPassed(equalOpportunity));
        assertEquals(18, countPassed(societyDiversity));
        List<Form> rejected = equalOpportunity.subList(18, 30);
        assertTrue(rejected.stream().allMatch(form -> form.getChangedToRegular() && form.getType() == FormType.REGULAR));
        assertTrue(rejected.stream().noneMatch(form -> form.getStatus() == FormStatus.PASSED));
    }

    @Test
    void 사회다양성이_미달이어도_기회균등_탈락자를_합쳐_사회통합_정원을_넘지_않는다() {
        // 배정: 기회균등 31 / 사회다양성 5
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 40, 60.0);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 5, 40.0);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectSecondPassUseCase.execute();

        assertEquals(36, countPassed(equalOpportunity) + countPassed(societyDiversity));
    }

    private void givenSpecialForms(List<Form> equalOpportunity, List<Form> societyDiversity) {
        List<Form> special = new ArrayList<>(equalOpportunity);
        special.addAll(societyDiversity);
        given(formRepository.findFirstPassedSpecialForm()).willReturn(special);
    }

    private List<Form> createForms(FormType type, int size) {
        return createForms(type, size, 50.0);
    }

    /**
     * secondRoundTop: 첫 원서의 자기주도학습+인성 점수 합. 이후 원서는 0.5점씩 낮아진다.
     */
    private List<Form> createForms(FormType type, int size, double secondRoundTop) {
        return IntStream.range(0, size)
                .mapToObj(i -> {
                    Form form = FormFixture.createForm(type);
                    ReflectionTestUtils.setField(form, "id", nextId++);
                    form.firstPass();
                    form.updateScore(new Score(130.0 - i * 0.5, 0));
                    double secondRound = secondRoundTop - i * 0.5;
                    form.getScore().updateSecondRoundScore(secondRound * 0.6, secondRound * 0.4);
                    return form;
                })
                .toList();
    }

    private long countPassed(List<Form> formList) {
        return formList.stream()
                .filter(form -> form.getStatus() == FormStatus.PASSED)
                .count();
    }

    private long countChangedToRegular(List<Form> formList) {
        return formList.stream()
                .filter(Form::getChangedToRegular)
                .count();
    }
}
