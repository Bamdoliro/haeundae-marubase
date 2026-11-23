package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.TestDataUniqueValueExhaustedException;
import com.bamdoliro.maru.domain.form.service.AssignExaminationNumberService;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.form.service.RandomFormGenerator;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.presentation.form.dto.request.GenerateTestFormRequest;
import com.bamdoliro.maru.presentation.form.dto.request.TestFormCountRequest;
import com.bamdoliro.maru.presentation.form.dto.response.GenerateTestFormResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GenerateTestFormUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FormRepository formRepository;

    @Mock
    private AssignExaminationNumberService assignExaminationNumberService;

    private RandomFormGenerator randomFormGenerator;

    private GenerateTestFormUseCase useCase;

    @BeforeEach
    void setUp() {
        randomFormGenerator = spy(new RandomFormGenerator(new Random(7)));
        useCase = new GenerateTestFormUseCase(userRepository, formRepository, randomFormGenerator, new CalculateFormScoreService(), assignExaminationNumberService);
    }

    @Test
    void 전형별로_지정한_개수만큼_최종제출_납부완료_상태의_테스트_원서를_만든다() {
        // given
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        AtomicLong nextExaminationNumber = new AtomicLong(111001L);
        willAnswer(invocation -> {
            invocation.<Form>getArgument(0).assignExaminationNumber(nextExaminationNumber.getAndIncrement());
            return null;
        }).given(assignExaminationNumberService).execute(any(Form.class));
        GenerateTestFormRequest request = new GenerateTestFormRequest(List.of(
                new TestFormCountRequest(FormType.REGULAR, 3),
                new TestFormCountRequest(FormType.ONE_PARENT, 2),
                new TestFormCountRequest(FormType.REGULAR, 1)
        ));

        // when
        GenerateTestFormResponse response = useCase.execute(request);

        // then
        assertEquals(6, response.getTotalCount());
        assertEquals(2, response.getFormList().size());
        assertEquals(FormType.REGULAR, response.getFormList().get(0).getType());
        assertEquals(4, response.getFormList().get(0).getCount());
        assertEquals(FormType.ONE_PARENT, response.getFormList().get(1).getType());
        assertEquals(2, response.getFormList().get(1).getCount());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(6)).save(userCaptor.capture());
        ArgumentCaptor<Form> formCaptor = ArgumentCaptor.forClass(Form.class);
        verify(formRepository, times(6)).save(formCaptor.capture());

        userCaptor.getAllValues().forEach(user -> assertTrue(user.isTestData()));
        List<Form> forms = formCaptor.getAllValues();
        forms.forEach(form -> {
            assertEquals(FormStatus.APPROVED, form.getStatus());
            assertTrue(form.getPayment());
            assertNotNull(form.getExaminationNumber());
            assertNotNull(form.getScore());
            assertTrue(form.getUser().isTestData());
        });
        assertEquals(6, forms.stream().map(form -> form.getApplicant().getRegistrationNumber()).distinct().count());
        assertEquals(6, forms.stream().map(form -> form.getUser().getPhoneNumber()).distinct().count());
        assertEquals(6, forms.stream().map(Form::getExaminationNumber).distinct().count());
        verify(assignExaminationNumberService, times(6)).execute(any(Form.class));
    }

    @Test
    void 테스트_원서는_확인_중_단계를_거쳐_승인_상태가_된다() {
        // given
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        Form[] generated = new Form[1];
        willAnswer(invocation -> {
            generated[0] = spy((Form) invocation.callRealMethod());
            return generated[0];
        }).given(randomFormGenerator).generate(any(User.class), eq(FormType.REGULAR), any(), anyString(), anyString());

        // when
        useCase.execute(new GenerateTestFormRequest(List.of(new TestFormCountRequest(FormType.REGULAR, 1))));

        // then
        Form form = generated[0];
        InOrder order = inOrder(form);
        order.verify(form).submit();
        order.verify(form).payFee();
        order.verify(form).receive();
        order.verify(form).approve();
        assertEquals(FormStatus.APPROVED, form.getStatus());
        verify(formRepository).save(form);
    }

    @Test
    void 이미_있는_전화번호와_주민등록번호는_다시_만든다() {
        // given
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(userRepository.existsByPhoneNumber(anyString())).willReturn(true, true, false);
        given(formRepository.existsByApplicant_RegistrationNumber(anyString())).willReturn(true, false);

        // when
        useCase.execute(new GenerateTestFormRequest(List.of(new TestFormCountRequest(FormType.REGULAR, 1))));

        // then
        verify(userRepository, times(3)).existsByPhoneNumber(anyString());
        verify(formRepository, times(2)).existsByApplicant_RegistrationNumber(anyString());
        verify(formRepository, times(1)).save(any(Form.class));
    }

    @Test
    void 같은_요청_안에서_이미_쓴_값이_다시_나오면_다시_만든다() {
        // given
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(randomFormGenerator.generatePhoneNumber()).willReturn("01000000001", "01000000001", "01000000002");

        // when
        useCase.execute(new GenerateTestFormRequest(List.of(new TestFormCountRequest(FormType.REGULAR, 2))));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        assertEquals(
                new HashSet<>(List.of("01000000001", "01000000002")),
                new HashSet<>(userCaptor.getAllValues().stream().map(User::getPhoneNumber).toList())
        );
    }

    @Test
    void 재시도_횟수_안에_겹치지_않는_값을_못_만들면_에러가_발생한다() {
        // given
        given(userRepository.existsByPhoneNumber(anyString())).willReturn(true);

        // when and then
        assertThrows(TestDataUniqueValueExhaustedException.class,
                () -> useCase.execute(new GenerateTestFormRequest(List.of(new TestFormCountRequest(FormType.REGULAR, 1)))));
        verify(userRepository, times(GenerateTestFormUseCase.MAX_UNIQUE_VALUE_ATTEMPTS)).existsByPhoneNumber(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(formRepository, never()).save(any(Form.class));
        verify(assignExaminationNumberService, never()).execute(any(Form.class));
    }
}
