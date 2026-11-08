package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.DuplicateRegistrationNumberException;
import com.bamdoliro.maru.domain.form.exception.FormAlreadySubmittedException;
import com.bamdoliro.maru.domain.form.service.AssignExaminationNumberService;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.form.service.ExaminationNumberLock;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.request.SubmitFormRequest;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.hibernate.exception.ConstraintViolationException;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitFormUseCaseTest {

    @InjectMocks
    private SubmitFormUseCase submitFormUseCase;

    @Mock
    private FormRepository formRepository;

    @Mock
    private CalculateFormScoreService calculateFormScoreService;

    @Mock
    private AssignExaminationNumberService assignExaminationNumberService;

    @Mock
    private ExaminationNumberLock examinationNumberLock;

    @Test
    void 원서를_제출한다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();

        given(formRepository.findByUser(user)).willReturn(Optional.empty());

        willDoNothing().given(assignExaminationNumberService).execute(any(Form.class));
        willDoNothing().given(calculateFormScoreService).execute(any(Form.class));

        // when
        submitFormUseCase.execute(user, request);

        // then
        verify(formRepository, times(1)).findByUser(user);
        verify(calculateFormScoreService, times(1)).execute(any(Form.class));
        verify(assignExaminationNumberService, times(1)).execute(any(Form.class));
        verify(formRepository, times(1)).save(any(Form.class));
    }

    @Test
    void 원서를_제출할_때_이미_제출한_원서가_반려상태가_아니면서_있으면_에러가_발생한다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);

        given(formRepository.findByUser(user)).willReturn(Optional.of(form));

        // when and then
        assertThrows(FormAlreadySubmittedException.class, () -> submitFormUseCase.execute(user, request));

        verify(formRepository, times(1)).findByUser(user);
        verify(calculateFormScoreService, never()).execute(any(Form.class));
        verify(assignExaminationNumberService, never()).execute(any(Form.class));
        verify(formRepository, never()).delete(any(Form.class));
        verify(formRepository, never()).save(any(Form.class));
    }

    @Test
    void 원서를_제출할_때_이미_등록된_주민등록번호면_에러가_발생한다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();
        String registrationNumber = request.getApplicant().getRegistrationNumber();

        given(formRepository.findByUser(user)).willReturn(Optional.empty());
        given(formRepository.existsByApplicant_RegistrationNumber(registrationNumber)).willReturn(true);

        // when and then
        assertThrows(DuplicateRegistrationNumberException.class, () -> submitFormUseCase.execute(user, request));

        verify(formRepository, times(1)).findByUser(user);
        verify(formRepository, times(1)).existsByApplicant_RegistrationNumber(registrationNumber);
        verify(calculateFormScoreService, never()).execute(any(Form.class));
        verify(assignExaminationNumberService, never()).execute(any(Form.class));
        verify(formRepository, never()).save(any(Form.class));
    }

    @Test
    void 중복_제출_검사보다_먼저_락을_잡는다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();
        given(formRepository.findByUser(user)).willReturn(Optional.empty());

        // when
        submitFormUseCase.execute(user, request);

        // then
        InOrder inOrder = inOrder(examinationNumberLock, formRepository, assignExaminationNumberService);
        inOrder.verify(examinationNumberLock).lock();
        inOrder.verify(formRepository).findByUser(user);
        inOrder.verify(assignExaminationNumberService).execute(any(Form.class));
        inOrder.verify(formRepository).save(any(Form.class));
    }

    @Test
    void 저장_시_user_id_유니크_제약_위반은_이미_제출한_원서_에러로_변환한다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();
        given(formRepository.findByUser(user)).willReturn(Optional.empty());
        given(formRepository.save(any(Form.class))).willThrow(uniqueViolation(
                "tbl_form_user_id_key",
                "ERROR: duplicate key value violates unique constraint \"tbl_form_user_id_key\" Detail: Key (user_id)=(1) already exists."
        ));

        // when and then
        assertThrows(FormAlreadySubmittedException.class, () -> submitFormUseCase.execute(user, request));
    }

    @Test
    void 저장_시_제약_이름을_모르면_메시지로_user_id_유니크_제약_위반을_판별한다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();
        given(formRepository.findByUser(user)).willReturn(Optional.empty());
        given(formRepository.save(any(Form.class))).willThrow(uniqueViolation(
                null,
                "ERROR: duplicate key value violates unique constraint \"some_other_name\" Detail: Key (user_id)=(1) already exists."
        ));

        // when and then
        assertThrows(FormAlreadySubmittedException.class, () -> submitFormUseCase.execute(user, request));
    }

    @Test
    void 저장_시_주민등록번호_유니크_제약_위반은_중복_주민등록번호_에러로_변환한다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();
        given(formRepository.findByUser(user)).willReturn(Optional.empty());
        given(formRepository.save(any(Form.class))).willThrow(uniqueViolation(
                "uk_ewrcmw7q2sycf9m1bq5g7njwm",
                "ERROR: duplicate key value violates unique constraint \"uk_ewrcmw7q2sycf9m1bq5g7njwm\" Detail: Key (registration_number)=(abc) already exists."
        ));

        // when and then
        assertThrows(DuplicateRegistrationNumberException.class, () -> submitFormUseCase.execute(user, request));
    }

    @Test
    void 저장_시_그_외_제약_위반은_그대로_던진다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();
        given(formRepository.findByUser(user)).willReturn(Optional.empty());
        given(formRepository.save(any(Form.class))).willThrow(uniqueViolation(
                "uk_34sdsp4bux2e7j1vy1ahqf3qh",
                "ERROR: duplicate key value violates unique constraint \"uk_34sdsp4bux2e7j1vy1ahqf3qh\" Detail: Key (examination_number)=(abc) already exists."
        ));

        // when and then
        assertThrows(DataIntegrityViolationException.class, () -> submitFormUseCase.execute(user, request));
    }

    @Test
    void 저장_시_INSERT_문에_user_id가_있어도_다른_컬럼의_제약_위반이면_그대로_던진다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();
        given(formRepository.findByUser(user)).willReturn(Optional.empty());
        given(formRepository.save(any(Form.class))).willThrow(uniqueViolation(
                "public.uk_34sdsp4bux2e7j1vy1ahqf3qh_index_8",
                "Unique index or primary key violation: \"PUBLIC.UK_34SDSP4BUX2E7J1VY1AHQF3QH_INDEX_8 ON PUBLIC.TBL_FORM(EXAMINATION_NUMBER NULLS FIRST) VALUES ( /* 1 */ 'abc' )\"; SQL statement: insert into tbl_form (examination_number, registration_number, user_id) values (?, ?, ?)"
        ));

        // when and then
        assertThrows(DataIntegrityViolationException.class, () -> submitFormUseCase.execute(user, request));
    }

    @Test
    void 저장_시_H2_형식의_user_id_유니크_제약_위반도_이미_제출한_원서_에러로_변환한다() {
        // given
        SubmitFormRequest request = FormFixture.createFormRequest(FormType.REGULAR);
        User user = UserFixture.createUser();
        given(formRepository.findByUser(user)).willReturn(Optional.empty());
        given(formRepository.save(any(Form.class))).willThrow(uniqueViolation(
                null,
                "Unique index or primary key violation: \"PUBLIC.UK_SOMETHING_INDEX_8 ON PUBLIC.TBL_FORM(USER_ID NULLS FIRST) VALUES ( /* 1 */ 1 )\"; SQL statement: insert into tbl_form (user_id) values (?)"
        ));

        // when and then
        assertThrows(FormAlreadySubmittedException.class, () -> submitFormUseCase.execute(user, request));
    }

    private DataIntegrityViolationException uniqueViolation(String constraintName, String message) {
        SQLException sqlException = new SQLException(message, "23505");
        return new DataIntegrityViolationException(
                "could not execute statement",
                new ConstraintViolationException("could not execute statement", sqlException, constraintName)
        );
    }
}
