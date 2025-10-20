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
class AssignInterviewNumberUseCaseTest {

    @InjectMocks
    private AssignInterviewNumberUseCase assignInterviewNumberUseCase;

    @Mock
    private FormFacade formFacade;

    @Test
    void 면접번호를_지정한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        Long newInterviewNumber = 123456L;
        given(formFacade.getForm(form.getId())).willReturn(form);

        // when
        assignInterviewNumberUseCase.execute(form.getId(), newInterviewNumber);

        // then
        verify(formFacade).getForm(form.getId());
        assertEquals(newInterviewNumber, form.getInterviewNumber());
    }

    @Test
    void 면접번호를_지정할_때_원서가_없으면_에러가_발생한다() {
        // given
        Long formId = 1L;
        Long newInterviewNumber = 123456L;
        willThrow(new FormNotFoundException()).given(formFacade).getForm(formId);

        // when and then
        assertThrows(FormNotFoundException.class, () ->
                assignInterviewNumberUseCase.execute(formId, newInterviewNumber));
    }
}