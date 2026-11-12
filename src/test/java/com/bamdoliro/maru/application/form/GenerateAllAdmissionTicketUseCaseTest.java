package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.pdf.GeneratePdfService;
import com.bamdoliro.maru.infrastructure.pdf.MergePdfService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.thymeleaf.ProcessTemplateService;
import com.bamdoliro.maru.shared.constants.Schedule;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.bamdoliro.maru.shared.fixture.SharedFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GenerateAllAdmissionTicketUseCaseTest {

    @InjectMocks
    private GenerateAllAdmissionTicketUseCase generateAllAdmissionTicketUseCase;

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    @Mock
    private ProcessTemplateService processTemplateService;

    @Mock
    private GeneratePdfService generatePdfService;

    @Mock
    private FileService fileService;

    @Mock
    private MergePdfService mergePdfService;

    @Mock
    private FormRepository formRepository;

    @Test
    void 모든_1차_합격자의_수험표를_생성한다() {
        // given
        List<Form> formList = new ArrayList<>();
        formList.add(FormFixture.createForm(FormType.REGULAR));
        formList.add(FormFixture.createForm(FormType.NATIONAL_VETERANS));
        formList.forEach(Form::firstPass);
        given(formRepository.findByStatus(FormStatus.FIRST_PASSED)).willReturn(formList);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), anyMap())).willReturn("html");
        given(fileService.getDownloadPresignedUrl(any(String.class), any(String.class))).willReturn(SharedFixture.createIdentificationPictureUrlResponse().getDownloadUrl());
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        generateAllAdmissionTicketUseCase.execute();

        //then
        verify(formRepository, times(1)).findByStatus(FormStatus.FIRST_PASSED);
        verify(processTemplateService, times(2)).execute(any(String.class), anyMap());
        verify(generatePdfService, times(2)).execute(any(String.class));
        verify(fileService, times(2)).getDownloadPresignedUrl(any(String.class), any(String.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 신분증_사진이_없는_1차_합격자가_있어도_모든_수험표를_생성한다() {
        // given
        List<Form> formList = new ArrayList<>();
        formList.add(FormFixture.createForm(FormType.REGULAR));
        formList.add(FormFixture.createForm(FormType.NATIONAL_VETERANS));
        formList.forEach(Form::firstPass);
        given(formRepository.findByStatus(FormStatus.FIRST_PASSED)).willReturn(formList);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), anyMap())).willReturn("html");
        given(fileService.getDownloadPresignedUrl(any(String.class), any(String.class)))
                .willReturn(null)
                .willReturn(SharedFixture.createIdentificationPictureUrlResponse().getDownloadUrl());
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        generateAllAdmissionTicketUseCase.execute();

        // then
        ArgumentCaptor<Map<String, Object>> formMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(formRepository, times(1)).findByStatus(FormStatus.FIRST_PASSED);
        verify(processTemplateService, times(2)).execute(any(String.class), formMapCaptor.capture());
        verify(generatePdfService, times(2)).execute(any(String.class));
        verify(fileService, times(2)).getDownloadPresignedUrl(any(String.class), any(String.class));

        List<Map<String, Object>> formMaps = formMapCaptor.getAllValues();
        assertNull(formMaps.get(0).get("identificationPictureUri"));
        assertNotNull(formMaps.get(1).get("identificationPictureUri"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 모든_수험표에_DB_입학_일정_값을_넣는다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);
        form.firstPass();
        given(formRepository.findByStatus(FormStatus.FIRST_PASSED)).willReturn(List.of(form));
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2031, true);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);
        given(processTemplateService.execute(any(String.class), anyMap())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        generateAllAdmissionTicketUseCase.execute();

        // then
        ArgumentCaptor<Map<String, Object>> formMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processTemplateService).execute(any(String.class), formMapCaptor.capture());
        Map<String, Object> formMap = formMapCaptor.getValue();
        assertEquals(2031, formMap.get("year"));
        assertEquals(Schedule.toLocaleString(ScheduleFixture.INTERVIEW_START), formMap.get("depthInterview"));
        assertEquals(Schedule.toLocaleString(ScheduleFixture.ANNOUNCEMENT_OF_FINAL_PASS), formMap.get("announcementOfSecondPass"));
        assertEquals(Schedule.toLocaleString(ScheduleFixture.REGISTRATION_START, ScheduleFixture.REGISTRATION_END), formMap.get("entranceRegistrationTime"));
    }
}
