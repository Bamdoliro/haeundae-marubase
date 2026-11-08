package com.bamdoliro.maru.infrastructure.aop.log;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.log.AdminFormStatusChangeLog;
import com.bamdoliro.maru.domain.log.AdminFromViewLog;
import com.bamdoliro.maru.domain.log.AdminLoginLog;
import com.bamdoliro.maru.domain.log.FormSubmitLog;
import com.bamdoliro.maru.domain.log.FormUpdateLog;
import com.bamdoliro.maru.domain.log.UpdatedField;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.user.domain.type.Authority;
import com.bamdoliro.maru.domain.user.service.UserFacade;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminFormStatusChangeLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminFormViewLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminLoginLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.FormSubmitLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.FormUpdateLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.UpdatedFieldRepository;
import com.bamdoliro.maru.presentation.auth.dto.request.LogInRequest;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Hibernate;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.ValueChange;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@RequiredArgsConstructor
@Aspect
@Component
public class LogAspect {

    private final Javers javers;
    private final FormFacade formFacade;
    private final UserFacade userFacade;
    private final EntityManager entityManager;
    private final AdminLoginLogRepository adminLoginLogRepository;
    private final FormSubmitLogRepository formSubmitLogRepository;
    private final FormUpdateLogRepository formUpdateLogRepository;
    private final UpdatedFieldRepository updatedFieldRepository;
    private final AdminFormViewLogRepository adminFormViewLogRepository;
    private final AdminFormStatusChangeLogRepository adminFormStatusChangeLogRepository;


    @AfterReturning(value = "execution(* com.bamdoliro.maru.application.auth.LogInUseCase.execute(..))")
    public void logAdminLoginSuccess(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String phoneNumber = ((LogInRequest) args[0]).getPhoneNumber();

        User user = userFacade.getUser(phoneNumber);

        if (user.getAuthority() == Authority.ADMIN) {
            HttpServletRequest request = getCurrentHttpRequest();

            String clientIp = getClientIp(request);
            String userAgent = getUserAgent(request);

            AdminLoginLog log = new AdminLoginLog(phoneNumber, clientIp, userAgent, user);
            adminLoginLogRepository.save(log);
        }
    }

    @AfterReturning(value = "execution(* com.bamdoliro.maru.application.form.SubmitFormUseCase.execute(..))")
    public void logSubmitFormSuccess(JoinPoint joinPoint) {
        generateFormSubmitLog(joinPoint, FormStatus.SUBMITTED);
    }

    @AfterReturning(value = "execution(* com.bamdoliro.maru.application.form.SubmitFinalFormUseCase.execute(..))")
    public void logSubmitFinalFormSuccess(JoinPoint joinPoint) {
        generateFormSubmitLog(joinPoint, FormStatus.FINAL_SUBMITTED);
    }

    @Around(value = "execution(* com.bamdoliro.maru.application.form.UpdateFormUseCase.execute(..))")
    public void logUpdateFormSuccess(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        User user = (User) args[0];
        Form originalForm = formFacade.getForm(user);
        Hibernate.initialize(originalForm.getGrade().getSubjectList().getValue());
        entityManager.detach(originalForm);

        joinPoint.proceed();

        Form updatedForm = formFacade.getForm(user);

        HttpServletRequest request = getCurrentHttpRequest();

        String clientIp = getClientIp(request);
        String userAgent = getUserAgent(request);

        FormUpdateLog formUpdateLog = FormUpdateLog.builder()
                .phoneNumber(user.getPhoneNumber())
                .clientIp(clientIp)
                .userAgent(userAgent)
                .user(user)
                .form(updatedForm)
                .build();
        formUpdateLogRepository.save(formUpdateLog);

        Diff diff = javers.compare(originalForm, updatedForm);

        diff.getChanges().forEach(change -> {
            if (change instanceof ValueChange valueChange) {
                String fieldName = valueChange.getPropertyName();
                String oldValue = Objects.toString(valueChange.getLeft(), "null");
                String newValue = Objects.toString(valueChange.getRight(), "null");
                UpdatedField updatedField = UpdatedField.builder()
                        .formUpdateLog(formUpdateLog)
                        .filedName(fieldName)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .build();
                updatedFieldRepository.save(updatedField);
            }
        });
    }

    /**
     * 관리자가 원서 상세를 조회하면 열람 이력을 남긴다. 지원자 본인 조회는 남기지 않는다.
     */
    @AfterReturning(value = "execution(* com.bamdoliro.maru.application.form.QueryFormUseCase.execute(..))")
    public void logAdminViewFormSuccess(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        User user = (User) args[0];

        if (user.getAuthority() != Authority.ADMIN) {
            return;
        }

        Long formId = (Long) args[1];
        Form form = formFacade.getForm(formId);

        HttpServletRequest request = getCurrentHttpRequest();

        AdminFromViewLog log = AdminFromViewLog.builder()
                .clientIp(getClientIp(request))
                .userAgent(getUserAgent(request))
                .user(user)
                .form(form)
                .build();
        adminFormViewLogRepository.save(log);
    }

    /**
     * 관리자가 원서를 승인/반려/접수 처리하면 변경 전후 상태를 남긴다.
     * 승인/반려/접수 UseCase는 처리한 관리자를 받지 않으므로 FormController 메서드에 건다.
     */
    @Around(
            value = "execution(* com.bamdoliro.maru.presentation.form.FormController.approveForm(..)) || " +
                    "execution(* com.bamdoliro.maru.presentation.form.FormController.rejectForm(..)) || " +
                    "execution(* com.bamdoliro.maru.presentation.form.FormController.receiveForm(..))"
    )
    public Object logChangeFormStatus(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        User user = (User) args[0];
        Long formId = (Long) args[1];

        HttpServletRequest request = getCurrentHttpRequest();
        String clientIp = getClientIp(request);
        String userAgent = getUserAgent(request);

        FormStatus beforeStatus = formFacade.getForm(formId).getStatus();

        Object result = joinPoint.proceed();

        // 처리 후 상태는 다시 조회한다. 같은 영속성 컨텍스트(open-in-view)면 같은 엔티티를,
        // 아니면 커밋된 상태를 읽으므로 어느 쪽이든 실제 변경 후 상태가 기록된다.
        Form form = formFacade.getForm(formId);

        AdminFormStatusChangeLog log = AdminFormStatusChangeLog.builder()
                .clientIp(clientIp)
                .userAgent(userAgent)
                .user(user)
                .form(form)
                .beforeStatus(beforeStatus)
                .afterStatus(form.getStatus())
                .build();
        adminFormStatusChangeLogRepository.save(log);

        return result;
    }

    private HttpServletRequest getCurrentHttpRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("x-forwarded-for");

        if (clientIp == null) return request.getRemoteAddr();
        return clientIp.split(",")[0];
    }

    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null) return "Unknown";
        return userAgent;
    }

    private void generateFormSubmitLog(JoinPoint joinPoint, FormStatus status) {
        Object[] args = joinPoint.getArgs();

        User user = (User) args[0];
        Form form = formFacade.getForm(user);

        HttpServletRequest request = getCurrentHttpRequest();

        String clientIp = getClientIp(request);
        String userAgent = getUserAgent(request);

        FormSubmitLog formSubmitLog = FormSubmitLog.builder()
                .phoneNumber(user.getPhoneNumber())
                .clientIp(clientIp)
                .userAgent(userAgent)
                .user(user)
                .form(form)
                .status(status)
                .build();
        formSubmitLogRepository.save(formSubmitLog);
    }
}
