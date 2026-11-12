package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.domain.form.exception.InvalidFormStatusException;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.pdf.GeneratePdfService;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.thymeleaf.ProcessTemplateService;
import com.bamdoliro.maru.shared.constants.Schedule;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.bamdoliro.maru.shared.fixture.SharedFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

public class GenerateProofOfApplicationUseCaseTest {

    @InjectMocks
    private GenerateProofOfApplicationUseCase generateProofOfApplicationUseCase;

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    @Mock
    private FormFacade formFacade;

    @Mock
    private ProcessTemplateService processTemplateService;

    @Mock
    private GeneratePdfService generatePdfService;

    @Mock
    private FileService fileService;

    @Test
    void 접수증을_생성한다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.MULTI_CHILDREN);
        form.submit();
        given(formFacade.getForm(user)).willReturn(form);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), anyMap())).willReturn("html");
        given(fileService.getDownloadPresignedUrl(any(String.class), any(String.class))).willReturn(SharedFixture.createIdentificationPictureUrlResponse().getDownloadUrl());
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        generateProofOfApplicationUseCase.execute(user);

        // then
        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, times(1)).execute(any(String.class), anyMap());
        verify(generatePdfService, times(1)).execute(any(String.class));
        verify(fileService, times(1)).getDownloadPresignedUrl(any(String.class), any(String.class));
    }

    @Test
    void 접수증을_생성할_때_상태가_최종제출_또는_승인_또는_접수_아닌_사용자라면_에러가_발생한다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.MULTI_CHILDREN);
        given(formFacade.getForm(user)).willReturn(form);

        // when and then
        assertThrows(InvalidFormStatusException.class, () -> generateProofOfApplicationUseCase.execute(user));

        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, never()).execute(any(String.class), anyMap());
        verify(generatePdfService, never()).execute(any(String.class));
    }

    @Test
    void 접수증을_생성할_때_원서를_접수하지_않았다면_에러가_발생한다() {
        // given
        User user = UserFixture.createUser();
        willThrow(new FormNotFoundException()).given(formFacade).getForm(user);

        // when and then
        assertThrows(FormNotFoundException.class, () -> generateProofOfApplicationUseCase.execute(user));

        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, never()).execute(any(String.class), anyMap());
        verify(generatePdfService, never()).execute(any(String.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 신분증_사진이_없어도_접수증을_생성한다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.MULTI_CHILDREN);
        form.submit();
        given(formFacade.getForm(user)).willReturn(form);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), anyMap())).willReturn("html");
        given(fileService.getDownloadPresignedUrl(any(String.class), any(String.class))).willReturn(null);
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        generateProofOfApplicationUseCase.execute(user);

        // then
        ArgumentCaptor<Map<String, Object>> formMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, times(1)).execute(any(String.class), formMapCaptor.capture());
        verify(generatePdfService, times(1)).execute(any(String.class));
        verify(fileService, times(1)).getDownloadPresignedUrl(any(String.class), any(String.class));

        Map<String, Object> formMap = formMapCaptor.getValue();
        assertTrue(formMap.containsKey("identificationPictureUri"));
        assertNull(formMap.get("identificationPictureUri"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 접수증에_DB_입학_일정_값을_넣는다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.MULTI_CHILDREN);
        form.submit();
        given(formFacade.getForm(user)).willReturn(form);
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2031, true);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);
        given(processTemplateService.execute(any(String.class), anyMap())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        generateProofOfApplicationUseCase.execute(user);

        // then
        ArgumentCaptor<Map<String, Object>> formMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processTemplateService).execute(any(String.class), formMapCaptor.capture());
        Map<String, Object> formMap = formMapCaptor.getValue();
        assertEquals(2031, formMap.get("year"));
        assertEquals(Schedule.toLocaleString(ScheduleFixture.ANNOUNCEMENT_OF_FIRST_PASS), formMap.get("announcement_of_first_pass"));
        assertEquals(Schedule.toLocaleString(ScheduleFixture.APPLICATION_FORM_START), formMap.get("start"));
        assertEquals(Schedule.toLocaleString(ScheduleFixture.APPLICATION_FORM_END), formMap.get("end"));
    }
}
