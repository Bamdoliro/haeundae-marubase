package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.schedule.domain.AdmissionSchedule;
import com.bamdoliro.maru.domain.schedule.service.AdmissionScheduleFacade;
import com.bamdoliro.maru.infrastructure.pdf.GeneratePdfService;
import com.bamdoliro.maru.infrastructure.pdf.MergePdfService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.thymeleaf.ProcessTemplateService;
import com.bamdoliro.maru.presentation.form.dto.response.FormSimpleResponse;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.ScheduleFixture;
import com.itextpdf.kernel.utils.PdfMerger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExportAllDocumentsUseCaseTest {

    @InjectMocks
    private ExportAllDocumentsUseCase exportAllDocumentsUseCase;

    @Mock
    private AdmissionScheduleFacade admissionScheduleFacade;

    @Mock
    private FormRepository formRepository;

    @Mock
    private ProcessTemplateService processTemplateService;

    @Mock
    private GeneratePdfService generatePdfService;

    @Mock
    private MergePdfService mergePdfService;

    @Test
    void 모든_원서를_하나의_PDF로_합쳐서_다운로드한다() {
        // given
        List<FormSimpleResponse> forms = List.of(
                FormFixture.createFormSimpleResponse(FormStatus.SUBMITTED),
                FormFixture.createFormSimpleResponse(FormStatus.SUBMITTED)
        );

        List<Form> formEntities = List.of(
                FormFixture.createForm(FormType.REGULAR),
                FormFixture.createForm(FormType.SPECIAL_ADMISSION)
        );

        given(formRepository.findAll()).willReturn(formEntities);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(ScheduleFixture.createSchedule(2027, true));
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());
        willDoNothing().given(mergePdfService).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));

        // when
        exportAllDocumentsUseCase.execute();

        // then
        verify(formRepository, times(1)).findAll();
        verify(processTemplateService, times(4)).execute(any(String.class), any());
        verify(generatePdfService, times(4)).execute(any(String.class));
        verify(mergePdfService, times(4)).execute(any(PdfMerger.class), any(ByteArrayOutputStream.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 서류에_DB_입학_일정의_학년도를_넣는다() {
        // given
        given(formRepository.findAll()).willReturn(List.of(FormFixture.createForm(FormType.REGULAR)));
        AdmissionSchedule schedule = ScheduleFixture.createSchedule(2031, true);
        given(admissionScheduleFacade.getCurrentSchedule()).willReturn(schedule);
        given(processTemplateService.execute(any(String.class), any())).willReturn("html");
        given(generatePdfService.execute(any(String.class))).willReturn(new ByteArrayOutputStream());

        // when
        exportAllDocumentsUseCase.execute();

        // then
        ArgumentCaptor<Map<String, Object>> documentMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processTemplateService, times(2)).execute(any(String.class), documentMapCaptor.capture());
        assertEquals(2031, documentMapCaptor.getValue().get("year"));
    }
}
