package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.pdf.GeneratePdfService;
import com.bamdoliro.maru.infrastructure.pdf.MergePdfService;
import com.bamdoliro.maru.infrastructure.pdf.exception.FailedToExportPdfException;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.thymeleaf.ProcessTemplateService;
import com.bamdoliro.maru.shared.config.TimeConfig;
import com.bamdoliro.maru.shared.constants.Schedule;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.bamdoliro.maru.shared.fixture.SharedFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import com.itextpdf.kernel.utils.PdfMerger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportFormUseCaseTest {

    @InjectMocks
    private ExportFormUseCase exportFormUseCase;

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    @Mock
    private FormFacade formFacade;

    @Mock
    private ProcessTemplateService processTemplateService;

    @Mock
    private GeneratePdfService generatePdfService;

    @Mock
    private MergePdfService mergePdfService;

    @Mock
    private FileService fileService;

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

    @Spy
    private Clock serviceClock = Clock.fixed(TODAY.atStartOfDay(TimeConfig.SERVICE_ZONE_ID).plusHours(10).toInstant(), TimeConfig.SERVICE_ZONE_ID);

    @Test
    void 일반전형_원서를_pdf로_다운받는다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formFacade.getForm(user)).willReturn(form);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());
        given(fileService.getDownloadPresignedUrl(any(String.class), any(String.class))).willReturn(SharedFixture.createIdentificationPictureUrlResponse().getDownloadUrl());
        willDoNothing().given(mergePdfService).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));

        // when
        exportFormUseCase.execute(user);

        // then
        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, times(5)).execute(any(String.class), any());
        verify(generatePdfService, times(5)).execute(any(String.class));
        verify(mergePdfService, times(5)).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));
        verify(fileService, times(1)).getDownloadPresignedUrl(any(String.class), any(String.class));
    }

    @Test
    void 일반전형이_아닌_원서를_pdf로_다운받는다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.FROM_NORTH_KOREA);
        given(formFacade.getForm(user)).willReturn(form);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());
        given(fileService.getDownloadPresignedUrl(any(String.class), any(String.class))).willReturn(SharedFixture.createIdentificationPictureUrlResponse().getDownloadUrl());
        willDoNothing().given(mergePdfService).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));

        // when
        exportFormUseCase.execute(user);

        // then
        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, times(6)).execute(any(String.class), any());
        verify(generatePdfService, times(6)).execute(any(String.class));
        verify(mergePdfService, times(6)).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));
        verify(fileService, times(1)).getDownloadPresignedUrl(any(String.class), any(String.class));
    }

    @Test
    void 원서를_pdf로_다운받을_때_모종의_이유로_변환_과정에서_실패하면_에러가_발생한다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formFacade.getForm(user)).willReturn(form);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(fileService.getDownloadPresignedUrl(any(String.class), any(String.class))).willReturn(SharedFixture.createIdentificationPictureUrlResponse().getDownloadUrl());
        doThrow(FailedToExportPdfException.class).when(generatePdfService).execute(any(String.class));

        // when and then
        assertThrows(FailedToExportPdfException.class, () -> exportFormUseCase.execute(user));

        // then
        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, times(1)).execute(any(String.class), any());
        verify(generatePdfService, times(1)).execute(any(String.class));
        verify(mergePdfService, never()).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));
        verify(fileService, times(1)).getDownloadPresignedUrl(any(String.class), any(String.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 신분증_사진이_없어도_원서를_pdf로_다운받는다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formFacade.getForm(user)).willReturn(form);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());
        given(fileService.getDownloadPresignedUrl(any(String.class), any(String.class))).willReturn(null);
        willDoNothing().given(mergePdfService).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));

        // when
        exportFormUseCase.execute(user);

        // then
        ArgumentCaptor<Map<String, Object>> formMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, times(5)).execute(any(String.class), formMapCaptor.capture());
        verify(generatePdfService, times(5)).execute(any(String.class));
        verify(mergePdfService, times(5)).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));
        verify(fileService, times(1)).getDownloadPresignedUrl(any(String.class), any(String.class));

        Map<String, Object> formMap = formMapCaptor.getValue();
        assertTrue(formMap.containsKey("identificationPictureUri"));
        assertNull(formMap.get("identificationPictureUri"));
        assertNotNull(formMap.get("form"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 원서에_DB_입학_일정_값을_넣는다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formFacade.getForm(user)).willReturn(form);
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2031, true);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        exportFormUseCase.execute(user);

        // then
        ArgumentCaptor<Map<String, Object>> formMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processTemplateService, atLeastOnce()).execute(any(String.class), formMapCaptor.capture());
        Map<String, Object> formMap = formMapCaptor.getValue();
        assertEquals(2031, formMap.get("year"));
        assertEquals(Schedule.toShortLocaleString(ScheduleFixture.APPLICATION_FORM_END), formMap.get("applicationDeadline"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 지원자가_출력하는_서류에는_출력하는_날짜를_넣는다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formFacade.getForm(user)).willReturn(form);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        exportFormUseCase.execute(user);

        // then
        ArgumentCaptor<Map<String, Object>> formMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processTemplateService, times(5)).execute(any(String.class), formMapCaptor.capture());
        assertEquals(TODAY, formMapCaptor.getValue().get("documentDate"));
    }
}
