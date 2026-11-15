package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminFormStatusChangeLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminFormViewLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminLoginLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.FormSubmitLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.FormUpdateLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.UpdatedFieldRepository;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.presentation.form.dto.response.DeleteTestFormResponse;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * isTestData = true 유저와 그 원서를 지운다. 실제 지원자(isTestData = false)는 건드리지 않는다.
 * FK 순서: 테스트 원서/유저를 참조하는 로그 -> 원서(과목, 자격증 컬렉션 포함) -> 유저.
 */
@RequiredArgsConstructor
@UseCase
public class DeleteTestFormUseCase {

    private final FormRepository formRepository;
    private final UserRepository userRepository;
    private final UpdatedFieldRepository updatedFieldRepository;
    private final FormUpdateLogRepository formUpdateLogRepository;
    private final FormSubmitLogRepository formSubmitLogRepository;
    private final AdminFormViewLogRepository adminFormViewLogRepository;
    private final AdminFormStatusChangeLogRepository adminFormStatusChangeLogRepository;
    private final AdminLoginLogRepository adminLoginLogRepository;

    @Transactional
    public DeleteTestFormResponse execute() {
        // 관리자가 테스트 원서를 조회하거나 상태를 바꾸면 로그가 원서를 참조하므로, 원서보다 먼저 지운다.
        int deletedLogCount = updatedFieldRepository.deleteAllReferencingTestData()
                + formUpdateLogRepository.deleteAllReferencingTestData()
                + formSubmitLogRepository.deleteAllReferencingTestData()
                + adminFormViewLogRepository.deleteAllReferencingTestData()
                + adminFormStatusChangeLogRepository.deleteAllReferencingTestData()
                + adminLoginLogRepository.deleteAllReferencingTestData();

        // 원서는 과목(tbl_subject), 자격증(tbl_certificate) 컬렉션 테이블이 있어 벌크 삭제 대신 엔티티 삭제로 함께 지운다.
        List<Form> forms = formRepository.findAllByUser_IsTestDataTrue();
        formRepository.deleteAll(forms);
        formRepository.flush();

        int deletedUserCount = userRepository.deleteAllTestData();

        return new DeleteTestFormResponse(forms.size(), deletedUserCount, deletedLogCount);
    }
}
