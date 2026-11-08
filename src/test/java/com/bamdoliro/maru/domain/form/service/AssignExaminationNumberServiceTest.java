package com.bamdoliro.maru.domain.form.service;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.ExaminationNumberExhaustedException;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssignExaminationNumberServiceTest {

    @InjectMocks
    private AssignExaminationNumberService assignExaminationNumberService;

    @Mock
    private FormRepository formRepository;

    @Mock
    private ExaminationNumberLock examinationNumberLock;

    @Test
    void 수험번호를_부여한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formRepository.findAllExaminationNumber()).willReturn(List.of(111001L, 111002L, 111003L, 111004L, 111005L));

        // when
        assignExaminationNumberService.execute(form);

        // then
        assertEquals(111006L, form.getExaminationNumber());
        verify(formRepository).findAllExaminationNumber();
    }

    @Test
    void 이전에_수험번호가_없다면_초기값을_부여한다() {
        // given
        Form form = FormFixture.createForm(FormType.TEEN_HOUSEHOLDER);
        given(formRepository.findAllExaminationNumber()).willReturn(List.of());

        // when
        assignExaminationNumberService.execute(form);

        // then
        assertEquals(213001L, form.getExaminationNumber());
        verify(formRepository).findAllExaminationNumber();
    }

    @Test
    void 수험번호를_계산하기_전에_락을_잡는다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formRepository.findAllExaminationNumber()).willReturn(List.of());

        // when
        assignExaminationNumberService.execute(form);

        // then
        InOrder inOrder = inOrder(examinationNumberLock, formRepository);
        inOrder.verify(examinationNumberLock).lock();
        inOrder.verify(formRepository).findAllExaminationNumber();
    }

    @Test
    void 구간의_시작값과_끝값은_수험번호_계산에서_제외한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formRepository.findAllExaminationNumber()).willReturn(List.of(111000L, 112000L, 112001L));

        // when
        assignExaminationNumberService.execute(form);

        // then
        assertEquals(111001L, form.getExaminationNumber());
    }

    @Test
    void 구간의_마지막_번호를_부여한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formRepository.findAllExaminationNumber()).willReturn(List.of(111997L, 111998L));

        // when
        assignExaminationNumberService.execute(form);

        // then
        assertEquals(111999L, form.getExaminationNumber());
    }

    @Test
    void 수험번호_구간이_꽉_차면_에러가_발생한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formRepository.findAllExaminationNumber()).willReturn(List.of(111998L, 111999L));

        // when and then
        assertThrows(ExaminationNumberExhaustedException.class, () -> assignExaminationNumberService.execute(form));
    }
}
