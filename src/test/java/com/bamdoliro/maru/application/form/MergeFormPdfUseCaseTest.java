package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.infrastructure.pdf.MergePdfService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.form.vo.FormUrlVo;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.infrastructure.s3.exception.S3FileNotFoundException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MergeFormPdfUseCaseTest {

    private MergeFormPdfUseCase mergeFormPdfUseCase;

    @Mock
    private FormRepository formRepository;

    @Mock
    private FileService fileService;

    private final UUID firstUuid = UUID.randomUUID();
    private final UUID secondUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // PDF 병합은 실제 MergePdfService로 수행한다.
        mergeFormPdfUseCase = new MergeFormPdfUseCase(formRepository, fileService, new MergePdfService());
    }

    private static byte[] createPdf(int pageCount) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfDocument document = new PdfDocument(new PdfWriter(outputStream));
        for (int i = 0; i < pageCount; i++) {
            document.addNewPage();
        }
        document.close();
        return outputStream.toByteArray();
    }

    private static int pageCount(ByteArrayResource resource) throws IOException {
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(resource.getByteArray())))) {
            return document.getNumberOfPages();
        }
    }

    @Test
    void 선택한_원서_PDF를_수험번호_순으로_합친다() throws IOException {
        List<Long> formIdList = List.of(1L, 2L);
        given(formRepository.findFormUrlByFormIdList(formIdList)).willReturn(List.of(
                new FormUrlVo(1002L, "두번째", secondUuid),
                new FormUrlVo(1001L, "첫번째", firstUuid)
        ));
        given(fileService.download(FolderConstant.FORM, firstUuid.toString())).willReturn(createPdf(2));
        given(fileService.download(FolderConstant.FORM, secondUuid.toString())).willReturn(createPdf(3));

        ByteArrayResource result = mergeFormPdfUseCase.execute(formIdList);

        assertEquals(5, pageCount(result));
        InOrder order = inOrder(fileService);
        order.verify(fileService).download(FolderConstant.FORM, firstUuid.toString());
        order.verify(fileService).download(FolderConstant.FORM, secondUuid.toString());
    }

    @Test
    void 같은_원서를_여러_번_요청해도_한_번만_합친다() throws IOException {
        List<Long> formIdList = List.of(1L, 1L);
        given(formRepository.findFormUrlByFormIdList(formIdList)).willReturn(List.of(
                new FormUrlVo(1001L, "첫번째", firstUuid)
        ));
        given(fileService.download(FolderConstant.FORM, firstUuid.toString())).willReturn(createPdf(2));

        assertEquals(2, pageCount(mergeFormPdfUseCase.execute(formIdList)));
    }

    @Test
    void 존재하지_않는_원서가_하나라도_있으면_전체가_실패한다() {
        List<Long> formIdList = List.of(1L, 999L);
        given(formRepository.findFormUrlByFormIdList(formIdList)).willReturn(List.of(
                new FormUrlVo(1001L, "첫번째", firstUuid)
        ));

        assertThrows(FormNotFoundException.class, () -> mergeFormPdfUseCase.execute(formIdList));
        verify(fileService, never()).download(anyString(), anyString());
    }

    @Test
    void 원서_목록이_비어_있으면_실패한다() {
        given(formRepository.findFormUrlByFormIdList(List.of())).willReturn(List.of());

        assertThrows(FormNotFoundException.class, () -> mergeFormPdfUseCase.execute(List.of()));
    }

    @Test
    void 원서_PDF_파일이_하나라도_없으면_전체가_실패한다() {
        List<Long> formIdList = List.of(1L, 2L);
        given(formRepository.findFormUrlByFormIdList(formIdList)).willReturn(List.of(
                new FormUrlVo(1001L, "첫번째", firstUuid),
                new FormUrlVo(1002L, "두번째", secondUuid)
        ));
        given(fileService.download(FolderConstant.FORM, firstUuid.toString())).willReturn(createPdf(1));
        given(fileService.download(FolderConstant.FORM, secondUuid.toString())).willThrow(new S3FileNotFoundException());

        assertThrows(S3FileNotFoundException.class, () -> mergeFormPdfUseCase.execute(formIdList));
    }
}
