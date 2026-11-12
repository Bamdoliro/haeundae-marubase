package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.InvalidFormStatusException;
import com.bamdoliro.maru.domain.form.exception.OutOfAdmissionAndPledgePeriodException;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.dto.request.FileMetadata;
import com.bamdoliro.maru.infrastructure.s3.validator.FileValidator;
import com.bamdoliro.maru.shared.config.TimeConfig;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.bamdoliro.maru.shared.fixture.SharedFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.bamdoliro.maru.shared.constants.FileConstant.MB;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class UploadAdmissionAndPledgeUseCaseTest {

    @Mock
    private FileService fileService;

    @Mock
    private FormFacade formFacade;

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    private UploadAdmissionAndPledgeUseCase useCaseAt(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(TimeConfig.SERVICE_ZONE_ID).toInstant(), TimeConfig.SERVICE_ZONE_ID);
        return new UploadAdmissionAndPledgeUseCase(fileService, formFacade, admissionScheduleFacade, clock);
    }

    private static FileMetadata pdfMetadata() {
        return new FileMetadata(
                "admission-and-pledge.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                10 * MB
        );
    }

    @Test
    void 입학등록원_및_금연서약서를_업로드한다() {
        //given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        form.pass();

        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(formFacade.getForm(user)).willReturn(form);
        given(fileService.getPresignedUrl(any(String.class), any(String.class), any(FileMetadata.class), any(FileValidator.class))).willReturn(SharedFixture.createAdmissionAndPledgeUrlResponse());

        //when
        useCaseAt(ScheduleFixture.ADMISSION_AND_PLEDGE_START.plusSeconds(1)).execute(user, pdfMetadata());

        //then
        verify(formFacade, times(1)).getForm(user);
        verify(fileService, times(1)).getPresignedUrl(any(String.class), any(String.class), any(FileMetadata.class), any(FileValidator.class));
    }

    @Test
    void 입학등록원_및_금연서약서를_업로드할_때_제출_기간이_아니면_에러가_발생한다() {
        // given
        User user = UserFixture.createUser();
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));

        // when and then
        assertThrows(OutOfAdmissionAndPledgePeriodException.class,
                () -> useCaseAt(ScheduleFixture.ADMISSION_AND_PLEDGE_START.minusSeconds(1)).execute(user, pdfMetadata()));
        assertThrows(OutOfAdmissionAndPledgePeriodException.class,
                () -> useCaseAt(ScheduleFixture.ADMISSION_AND_PLEDGE_END.plusSeconds(1)).execute(user, pdfMetadata()));

        verify(formFacade, never()).getForm(user);
        verify(fileService, never()).getPresignedUrl(any(String.class), any(String.class), any(FileMetadata.class), any(FileValidator.class));
    }

    @Test
    void 제출_기간이_설정되지_않은_일정이면_에러가_발생한다() {
        // given
        User user = UserFixture.createUser();
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2027, true);
        ReflectionTestUtils.setField(schedule, "admissionAndPledgeStart", null);
        ReflectionTestUtils.setField(schedule, "admissionAndPledgeEnd", null);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);

        // when and then
        assertThrows(OutOfAdmissionAndPledgePeriodException.class,
                () -> useCaseAt(ScheduleFixture.ADMISSION_AND_PLEDGE_START.plusSeconds(1)).execute(user, pdfMetadata()));
        verify(formFacade, never()).getForm(user);
    }

    @Test
    void 최종합격자가_아닌_지원자가_입학등록원_및_금연서약서를_업로드하면_에러가_발생한다() {
        //given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);

        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(formFacade.getForm(user)).willReturn(form);

        //when
        assertThrows(InvalidFormStatusException.class,
                () -> useCaseAt(ScheduleFixture.ADMISSION_AND_PLEDGE_START.plusSeconds(1)).execute(user, pdfMetadata()));

        //then
        verify(formFacade, times(1)).getForm(user);
        verify(fileService, never()).getPresignedUrl(any(String.class), any(String.class), any(FileMetadata.class), any(FileValidator.class));
    }
}
