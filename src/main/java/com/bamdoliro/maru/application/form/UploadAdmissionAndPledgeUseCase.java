package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.exception.InvalidFormStatusException;
import com.bamdoliro.maru.domain.form.exception.OutOfAdmissionAndPledgePeriodException;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.infrastructure.s3.dto.request.FileMetadata;
import com.bamdoliro.maru.infrastructure.s3.dto.response.UrlResponse;
import com.bamdoliro.maru.infrastructure.s3.validator.DefaultFileValidator;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@RequiredArgsConstructor
@UseCase
public class UploadAdmissionAndPledgeUseCase {

    private final FileService fileService;
    private final FormFacade formFacade;
    private final AdmissionScheduleFacade admissionScheduleFacade;
    private final Clock serviceClock;

    public UrlResponse execute(User user, FileMetadata fileMetadata) {
        validateApplicationPeriod(admissionScheduleFacade.getCurrentSchedule(), LocalDateTime.now(serviceClock));
        Form form = formFacade.getForm(user);
        validateFormStatus(form);

        return fileService.getPresignedUrl(FolderConstant.ADMISSION_AND_PLEDGE, user.getUuid().toString(), fileMetadata, metadata ->
                DefaultFileValidator.validate(metadata, Set.of(MediaType.APPLICATION_PDF), 20)
        );
    }

    /**
     * 제출 기간이 아직 설정되지 않은 일정(배포 전 저장된 행)이면 기간 밖으로 본다.
     */
    private void validateApplicationPeriod(AdmissionSchedule schedule, LocalDateTime now) {
        LocalDateTime start = schedule.getAdmissionAndPledgeStart();
        LocalDateTime end = schedule.getAdmissionAndPledgeEnd();
        if (Objects.isNull(start) || Objects.isNull(end) || now.isBefore(start) || now.isAfter(end)) {
            throw new OutOfAdmissionAndPledgePeriodException();
        }
    }

    private void validateFormStatus(Form form) {
        if(!form.isPassedNow() && !form.isEntered())
            throw new InvalidFormStatusException();
    }
}
