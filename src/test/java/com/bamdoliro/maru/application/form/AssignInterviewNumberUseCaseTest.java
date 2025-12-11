package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.request.AssignInterviewNumberListRequest;
import com.bamdoliro.maru.presentation.form.dto.request.AssignInterviewNumberRequest;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssignInterviewNumberUseCaseTest {

    @InjectMocks
    private AssignInterviewNumberUseCase assignInterviewNumberUseCase;

    @Mock
    private FormRepository formRepository;

    @Test
    void 면접번호를_일괄_지정한다() {
        // given
        Form form1 = FormFixture.createForm(FormType.REGULAR);
        Form form2 = FormFixture.createForm(FormType.NATIONAL_VETERANS);
        List<Form> formList = List.of(form1, form2);

        List<AssignInterviewNumberRequest> requestList = List.of(
                new AssignInterviewNumberRequest(1L, "1001"),
                new AssignInterviewNumberRequest(2L, "1002")
        );
        AssignInterviewNumberListRequest request = new AssignInterviewNumberListRequest(requestList);

        given(formRepository.findByFormIdList(List.of(1L, 2L))).willReturn(formList);

        // when
        assignInterviewNumberUseCase.execute(request);

        // then
        verify(formRepository).findByFormIdList(List.of(1L, 2L));
        assertEquals("1001", form1.getInterviewNumber());
        assertEquals("1002", form2.getInterviewNumber());
    }

    @Test
    void 면접번호를_지정할_때_원서가_없으면_에러가_발생한다() {
        // given
        List<AssignInterviewNumberRequest> requestList = List.of(
                new AssignInterviewNumberRequest(1L, "1001"),
                new AssignInterviewNumberRequest(2L, "1002")
        );
        AssignInterviewNumberListRequest request = new AssignInterviewNumberListRequest(requestList);

        given(formRepository.findByFormIdList(List.of(1L, 2L))).willReturn(List.of()); // 빈 리스트

        // when and then
        assertThrows(FormNotFoundException.class, () ->
                assignInterviewNumberUseCase.execute(request));
    }

    @Test
    void 면접번호가_null인_경우_건너뛴다() {
        // given
        Form form1 = FormFixture.createForm(FormType.REGULAR);
        Form form2 = FormFixture.createForm(FormType.NATIONAL_VETERANS);
        List<Form> formList = List.of(form1, form2);

        List<AssignInterviewNumberRequest> requestList = List.of(
                new AssignInterviewNumberRequest(1L, "1001"),
                new AssignInterviewNumberRequest(2L, null) // null 면접번호
        );
        AssignInterviewNumberListRequest request = new AssignInterviewNumberListRequest(requestList);

        given(formRepository.findByFormIdList(List.of(1L, 2L))).willReturn(formList);

        // when
        assignInterviewNumberUseCase.execute(request);

        // then
        assertEquals("1001", form1.getInterviewNumber());
        assertEquals(null, form2.getInterviewNumber()); // null로 유지
    }
}