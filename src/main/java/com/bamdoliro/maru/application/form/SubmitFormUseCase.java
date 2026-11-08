package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.exception.DuplicateRegistrationNumberException;
import com.bamdoliro.maru.domain.form.exception.FormAlreadySubmittedException;
import com.bamdoliro.maru.domain.form.service.AssignExaminationNumberService;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.form.service.ExaminationNumberLock;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.form.dto.request.SubmitFormRequest;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.annotation.ValidateApplicationFormPeriod;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@UseCase
public class SubmitFormUseCase {

    /**
     * tbl_form.registration_number 유니크 제약. Hibernate가 생성한 이름이며 PostgreSQL에서는 소문자로 저장된다.
     */
    private static final String REGISTRATION_NUMBER_CONSTRAINT = "uk_ewrcmw7q2sycf9m1bq5g7njwm";

    /**
     * user_id 유니크 제약. 현재 엔티티 설정으로는 생성되지 않지만, 수동으로 추가했을 때의 PostgreSQL 기본 이름이다.
     */
    private static final String USER_CONSTRAINT = "tbl_form_user_id_key";

    private final FormRepository formRepository;
    private final CalculateFormScoreService calculateFormScoreService;
    private final AssignExaminationNumberService assignExaminationNumberService;
    private final ExaminationNumberLock examinationNumberLock;

    @ValidateApplicationFormPeriod
    @Transactional
    public void execute(User user, SubmitFormRequest request) {
        // 중복 제출 검사부터 저장까지 한 번에 하나의 제출만 진행되도록 직렬화한다.
        // tbl_form.user_id에는 유니크 제약이 없으므로, 같은 사용자의 동시 제출은 이 락과 아래 검사로 막는다.
        examinationNumberLock.lock();
        validateOnlyOneFormPerUser(user);
        validateRegistrationNumberNotDuplicated(request.getApplicant().getRegistrationNumber());

        Form form = Form.builder()
                .applicant(request.getApplicant().toValue())
                .parent(request.getParent().toValue())
                .education(request.getEducation().toValue())
                .grade(request.getGrade().toValue())
                .document(request.getDocument().toValue())
                .type(request.getType())
                .user(user)
                .build();

        calculateFormScoreService.execute(form);
        assignExaminationNumberService.execute(form);

        try {
            formRepository.save(form);
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    private void validateOnlyOneFormPerUser(User user) {
        Optional<Form> form = formRepository.findByUser(user);
        if (form.isPresent()) {
            throw new FormAlreadySubmittedException();
        }
    }

    private void validateRegistrationNumberNotDuplicated(String registrationNumber) {
        if (formRepository.existsByApplicant_RegistrationNumber(registrationNumber)) {
            throw new DuplicateRegistrationNumberException();
        }
    }

    /**
     * id 전략이 IDENTITY라 save() 시점에 INSERT가 실행되므로 유니크 제약 위반도 여기서 발생한다.
     * 제약 이름으로 먼저 판별하고, 이름으로 판별되지 않으면 위반된 키 컬럼(메시지)으로 판별한다.
     */
    private RuntimeException translate(DataIntegrityViolationException e) {
        String constraintName = extractConstraintName(e);
        String message = e.getMostSpecificCause().getMessage();
        message = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (isConstraint(constraintName, USER_CONSTRAINT) || isUniqueViolationOf(message, "user_id")) {
            return new FormAlreadySubmittedException();
        }
        if (isConstraint(constraintName, REGISTRATION_NUMBER_CONSTRAINT) || isUniqueViolationOf(message, "registration_number")) {
            return new DuplicateRegistrationNumberException();
        }
        return e;
    }

    private String extractConstraintName(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return violation.getConstraintName().toLowerCase(Locale.ROOT);
            }
            cause = cause.getCause();
        }
        return null;
    }

    /**
     * DB에 따라 제약 이름 앞에 스키마나 따옴표가, 뒤에 인덱스 접미사와 위반 내용이 붙어 오므로 앞부분만 비교한다.
     */
    private boolean isConstraint(String constraintName, String expected) {
        if (constraintName == null) {
            return false;
        }
        String name = constraintName.replace("\"", "");
        if (name.startsWith("public.")) {
            name = name.substring("public.".length());
        }
        return name.equals(expected) || name.startsWith(expected + "_index") || name.startsWith(expected + " ");
    }

    /**
     * 위반된 키 컬럼만 본다. 메시지 전체에서 컬럼명을 찾으면 함께 실린 INSERT 문의 컬럼 목록까지 걸리므로 쓰지 않는다.
     * PostgreSQL: Key (user_id)=(1) already exists.
     * H2: ... ON PUBLIC.TBL_FORM(USER_ID NULLS FIRST) ...
     */
    private boolean isUniqueViolationOf(String message, String column) {
        return (message.contains("key (" + column + ")=(") && message.contains("already exists"))
                || message.contains("tbl_form(" + column + " ");
    }
}
