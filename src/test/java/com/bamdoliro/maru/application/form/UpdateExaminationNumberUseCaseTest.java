package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateExaminationNumberUseCaseTest {

    @InjectMocks
    private UpdateExaminationNumberUseCase updateExaminationNumberUseCase;

    @Mock
    private FormFacade formFacade;

    @Test
    void 수험번호를_수정한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        Long newExaminationNumber = 123456L;
        given(formFacade.getForm(form.getId())).willReturn(form);

        // when
        updateExaminationNumberUseCase.execute(form.getId(), newExaminationNumber);

        // then
        verify(formFacade).getForm(form.getId());
        assertEquals(newExaminationNumber, form.getExaminationNumber());
    }

    @Test
    void 수험번호를_수정할_때_원서가_없으면_에러가_발생한다() {
        // given
        Long formId = 1L;
        Long newExaminationNumber = 123456L;
        willThrow(new FormNotFoundException()).given(formFacade).getForm(formId);

        // when and then
        assertThrows(FormNotFoundException.class, () ->
            updateExaminationNumberUseCase.execute(formId, newExaminationNumber));
    }
}