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
    private long nextId = 1;

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

    @Test
    void 기회균등_탈락자도_점수가_높으면_사회다양성_정원으로_합격한다() {
        // 기회균등 27명 우선선발, 탈락 13명(136.5~130.5점)이 사회다양성 40명(130~110.5점)과 27자리를 놓고 경쟁
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 40, 150.0);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 40, 130.0);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectFirstPassUseCase.execute();

        assertEquals(40, countFirstPassed(equalOpportunity));
        assertEquals(14, countFirstPassed(societyDiversity));
        assertEquals(0, countChangedToRegular(equalOpportunity));
        assertEquals(26, countChangedToRegular(societyDiversity));
        // 사회다양성 정원으로 합격해도 원래 전형은 그대로 유지한다.
        assertTrue(equalOpportunity.stream().allMatch(form -> form.getType() == FormType.NATIONAL_BASIC_LIVING));
    }

    @Test
    void 기회균등_탈락자가_사회다양성_정원에서도_밀리면_일반전형으로_전환된다() {
        // 기회균등 탈락 13명(73~61점)은 사회다양성 상위 27명(130~117점)에 밀린다.
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 40, 100.0, 1.0);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 40, 130.0);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectFirstPassUseCase.execute();

        assertEquals(27, countFirstPassed(equalOpportunity));
        assertEquals(27, countFirstPassed(societyDiversity));
        List<Form> rejected = equalOpportunity.subList(27, 40);
        assertTrue(rejected.stream().allMatch(form -> form.getChangedToRegular() && form.getType() == FormType.REGULAR));
        assertTrue(rejected.stream().noneMatch(form -> form.getStatus() == FormStatus.FIRST_PASSED));
    }

    @Test
    void 사회다양성이_미달이어도_기회균등_탈락자를_합쳐_사회통합_정원을_넘지_않는다() {
        // 배정: 기회균등 44 / 사회다양성 10, 기회균등 탈락 16명은 사회다양성 10명과 10자리를 놓고 경쟁
        List<Form> equalOpportunity = createForms(FormType.NATIONAL_BASIC_LIVING, 60, 150.0);
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 10, 100.0);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectFirstPassUseCase.execute();

        assertEquals(54, countFirstPassed(equalOpportunity) + countFirstPassed(societyDiversity));
        assertEquals(54, countFirstPassed(equalOpportunity));
        assertEquals(0, countFirstPassed(societyDiversity));
    }

    @Test
    void 기회균등_우선선발에서도_마지막_합격자와_동점이면_모두_합격한다() {
        // 기회균등 30명: 26명은 150~125점, 27~29번째는 120점 동점, 30번째는 110점
        List<Form> equalOpportunity = new ArrayList<>(createForms(FormType.NATIONAL_BASIC_LIVING, 26, 150.0, 1.0));
        equalOpportunity.addAll(createForms(FormType.NATIONAL_BASIC_LIVING, 3, 120.0, 0.0));
        equalOpportunity.addAll(createForms(FormType.NATIONAL_BASIC_LIVING, 1, 110.0));
        // 사회다양성 상위 27명은 130~117점이라 기회균등 탈락자(110점)는 사회다양성에서도 밀린다.
        List<Form> societyDiversity = createForms(FormType.MULTICULTURAL, 40, 130.0);
        givenSpecialForms(equalOpportunity, societyDiversity);

        selectFirstPassUseCase.execute();

        assertTrue(equalOpportunity.subList(0, 29).stream()
                .allMatch(form -> form.getStatus() == FormStatus.FIRST_PASSED && !form.getChangedToRegular()));
        assertTrue(equalOpportunity.get(29).getChangedToRegular());
        assertEquals(27, countFirstPassed(societyDiversity));
    }

    private void givenSpecialForms(List<Form> equalOpportunity, List<Form> societyDiversity) {
        List<Form> special = new ArrayList<>(equalOpportunity);
        special.addAll(societyDiversity);
        given(formRepository.findApprovedSpecialForm()).willReturn(special);
    }

    private List<Form> createForms(FormType type, int size) {
        return createForms(type, size, 130.0);
    }

    private List<Form> createForms(FormType type, int size, double topScore) {
        return createForms(type, size, topScore, 0.5);
    }

    private List<Form> createForms(FormType type, int size, double topScore, double step) {
        return IntStream.range(0, size)
                .mapToObj(i -> {
                    Form form = FormFixture.createForm(type);
                    ReflectionTestUtils.setField(form, "id", nextId++);
                    form.approve();
                    form.payFee();
                    form.updateScore(new Score(topScore - i * step, 0));
                    return form;
                })
                .toList();
    }

    private long countFirstPassed(List<Form> formList) {
        return formList.stream()
                .filter(form -> form.getStatus() == FormStatus.FIRST_PASSED)
                .count();
    }

    private long countChangedToRegular(List<Form> formList) {
        return formList.stream()
                .filter(Form::getChangedToRegular)
                .count();
    }
}
