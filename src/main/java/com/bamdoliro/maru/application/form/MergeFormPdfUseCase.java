package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.exception.FormNotFoundException;
import com.bamdoliro.maru.infrastructure.pdf.MergePdfService;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.form.vo.FormUrlVo;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.infrastructure.s3.constants.FolderConstant;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.List;

/**
 * 선택한 원서들의 PDF를 수험번호 순으로 하나로 합친다.
 * 존재하지 않는 원서가 하나라도 있거나, 원서 PDF 파일이 하나라도 없으면 전체가 실패한다.
 */
@RequiredArgsConstructor
@UseCase
public class MergeFormPdfUseCase {

    private final FormRepository formRepository;
    private final FileService fileService;
    private final MergePdfService mergePdfService;

    public ByteArrayResource execute(List<Long> formIdList) {
        List<FormUrlVo> formList = formRepository.findFormUrlByFormIdList(formIdList)
                .stream()
                .sorted(Comparator.comparing(
                        FormUrlVo::getExaminationNumber,
                        Comparator.nullsLast(Long::compareTo)
                ))
                .toList();
        validateAllFormsExist(formIdList, formList);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfDocument mergedDocument = new PdfDocument(new PdfWriter(outputStream));
        PdfMerger pdfMerger = new PdfMerger(mergedDocument);

        formList.forEach(form -> mergePdfService.execute(
                pdfMerger,
                toOutputStream(fileService.download(FolderConstant.FORM, form.getUuid()))
        ));

        mergedDocument.close();
        pdfMerger.close();

        return new ByteArrayResource(outputStream.toByteArray());
    }

    private void validateAllFormsExist(List<Long> formIdList, List<FormUrlVo> formList) {
        long distinctFormCount = formIdList.stream().distinct().count();
        if (formIdList.isEmpty() || formList.size() != distinctFormCount) {
            throw new FormNotFoundException();
        }
    }

    private ByteArrayOutputStream toOutputStream(byte[] bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bytes.length);
        outputStream.writeBytes(bytes);
        return outputStream;
    }
}
