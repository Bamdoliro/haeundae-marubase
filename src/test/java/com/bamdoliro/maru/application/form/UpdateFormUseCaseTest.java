package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.auth.exception.AuthorityMismatchException;
import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.CannotUpdateNotRejectedFormException;
import com.bamdoliro.maru.domain.form.exception.DuplicateRegistrationNumberException;
import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.request.UpdateFormRequest;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateFormUseCaseTest {

    @InjectMocks
    private UpdateFormUseCase updateFormUseCase;

    @Mock
    private FormFacade formFacade;

    @Mock
    private CalculateFormScoreService calculateFormScoreService;

    @Mock
    private FormRepository formRepository;

    @Test
    void 원서를_수정한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        form.reject();
        User user = form.getUser();

        given(formFacade.getForm(user)).willReturn(form);

        // when
        updateFormUseCase.execute(user, FormFixture.createUpdateFormRequest(FormType.NATIONAL_VETERANS));

        // then
        verify(formFacade, times(1)).getForm(user);
        assertEquals(FormType.NATIONAL_VETERANS, form.getType());
    }

    @Test
    void 원서를_수정할_때_원서가_없으면_에러가_발생한다() {
        // given
        User user = UserFixture.createUser();

        willThrow(new FormNotFoundException()).given(formFacade).getForm(user);

        // when and then
        assertThrows(FormNotFoundException.class, () ->
                updateFormUseCase.execute(user, FormFixture.createUpdateFormRequest(FormType.NATIONAL_VETERANS)));

        verify(formFacade, times(1)).getForm(user);
    }

    @Test
    void 원서를_수정할_때_본인의_원서가_아니면_에러가_발생한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        form.reject();
        User otherUser = UserFixture.createUser();

        given(formFacade.getForm(otherUser)).willReturn(form);

        // when and then
        assertThrows(AuthorityMismatchException.class, () ->
                updateFormUseCase.execute(otherUser, FormFixture.createUpdateFormRequest(FormType.NATIONAL_VETERANS))
        );

        verify(formFacade, times(1)).getForm(otherUser);
    }

    @Test
    void 원서를_수정할_때_반려된_원서가_아니면_에러가_발생한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        User user = form.getUser();

        given(formFacade.getForm(user)).willReturn(form);

        // when and then
        assertThrows(CannotUpdateNotRejectedFormException.class, () ->
                updateFormUseCase.execute(user, FormFixture.createUpdateFormRequest(FormType.NATIONAL_VETERANS))
        );

        verify(formFacade, times(1)).getForm(user);
    }

    @Test
    void 원서를_수정할_때_이미_등록된_다른_주민등록번호면_에러가_발생한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        form.reject();
        User user = form.getUser();
        UpdateFormRequest request = FormFixture.createUpdateFormRequest(FormType.NATIONAL_VETERANS);
        String registrationNumber = request.getApplicant().getRegistrationNumber();

        given(formFacade.getForm(user)).willReturn(form);
        given(formRepository.existsByApplicant_RegistrationNumber(registrationNumber)).willReturn(true);

        // when and then
        assertThrows(DuplicateRegistrationNumberException.class, () -> updateFormUseCase.execute(user, request));

        verify(formFacade, times(1)).getForm(user);
        verify(formRepository, times(1)).existsByApplicant_RegistrationNumber(registrationNumber);
        verify(formRepository, never()).save(any(Form.class));
        assertEquals(FormType.REGULAR, form.getType());
    }

    @Test
    void 원서를_수정할_때_기존_본인의_주민등록번호는_중복으로_취급하지_않는다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        form.reject();
        User user = form.getUser();
        UpdateFormRequest request = FormFixture.createUpdateFormRequest(FormType.NATIONAL_VETERANS);
        ReflectionTestUtils.setField(request.getApplicant(), "registrationNumber",
                form.getApplicant().getRegistrationNumber());

        given(formFacade.getForm(user)).willReturn(form);

        // when
        updateFormUseCase.execute(user, request);

        // then
        verify(formRepository, never()).existsByApplicant_RegistrationNumber(anyString());
        verify(formRepository, times(1)).save(form);
        assertEquals(FormType.NATIONAL_VETERANS, form.getType());
    }
}
