package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.InvalidFormStatusException;
import com.bamdoliro.maru.domain.form.service.FormFacade;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.pdf.GeneratePdfService;
import com.bamdoliro.maru.infrastructure.pdf.MergePdfService;
import com.bamdoliro.maru.infrastructure.pdf.exception.FailedToExportPdfException;
import com.bamdoliro.maru.infrastructure.thymeleaf.ProcessTemplateService;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import com.itextpdf.kernel.utils.PdfMerger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class DownloadAdmissionAndPledgeFormatUseCaseTest {

    @InjectMocks
    private DownloadAdmissionAndPledgeFormatUseCase downloadAdmissionAndPledgeFormatUseCase;

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

    @Test
    void 최종합격한_지원자는_입학등록원_및_금연서약서_양식을_pdf로_다운받는다() {
        //given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        form.pass();
        given(formFacade.getForm(user)).willReturn(form);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());
        willDoNothing().given(mergePdfService).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));

        //when
        downloadAdmissionAndPledgeFormatUseCase.execute(user);

        //then
        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, times(2)).execute(any(String.class), any());
        verify(generatePdfService, times(2)).execute(any(String.class));
        verify(mergePdfService, times(2)).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));
    }

    @Test
    void 최종합격하지않은_지원자가_입학등록원_및_금연서약서_양식을_다운받으면_에러가_발생한다() {
        //given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        given(formFacade.getForm(user)).willReturn(form);

        //when and then
        assertThrows(InvalidFormStatusException.class, () -> downloadAdmissionAndPledgeFormatUseCase.execute(user));

        //then
        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, never()).execute(any(String.class), any());
        verify(generatePdfService, never()).execute(any(String.class));
        verify(mergePdfService, never()).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));
    }

    @Test
    void 입학등록원_및_금연서약서_양식을_다운받을_때_모종의_이유로_변환_과정에서_실패하면_에러가_발생한다() {
        //given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        form.pass();
        given(formFacade.getForm(user)).willReturn(form);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        doThrow(FailedToExportPdfException.class).when(generatePdfService).execute(any(String.class));

        //when and then
        assertThrows(FailedToExportPdfException.class, () -> downloadAdmissionAndPledgeFormatUseCase.execute(user));

        //then
        verify(formFacade, times(1)).getForm(user);
        verify(processTemplateService, times(1)).execute(any(String.class), any());
        verify(generatePdfService, times(1)).execute(any(String.class));
        verify(mergePdfService, never()).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 입학등록원_양식에_DB_입학_일정의_학년도를_넣는다() {
        // given
        User user = UserFixture.createUser();
        Form form = FormFixture.createForm(FormType.REGULAR);
        form.pass();
        given(formFacade.getForm(user)).willReturn(form);
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2031, true);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        downloadAdmissionAndPledgeFormatUseCase.execute(user);

        // then
        ArgumentCaptor<Map<String, Object>> formMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processTemplateService, times(2)).execute(any(String.class), formMapCaptor.capture());
        assertEquals(2031, formMapCaptor.getValue().get("year"));
    }
}
