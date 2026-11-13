package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.value.Document;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.xlsx.XlsxGenerator;
import com.bamdoliro.maru.infrastructure.xlsx.constant.XlsxConstant;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@RequiredArgsConstructor
@UseCase
public class ExportAllPersonalStatementSheetUseCase {

    private static final String TEMPLATE_NAME = "자기소개서";
    private static final String ANSWER_SEPARATOR = "\n\n";

    private final FormRepository formRepository;
    private final XlsxGenerator xlsxGenerator;

    public Resource execute() throws IOException {
        // 자기소개서 PDF 전체 발급(ExportAllDocumentsUseCase)과 같은 순서를 쓰기 위해 findAll() 기본 순서를 그대로 사용한다.
        List<Form> formList = formRepository.findAll();

        List<Function<Form, Object>> columnList = List.of(
                Form::getExaminationNumber,
                form -> form.getApplicant().getName(),
                ExportAllPersonalStatementSheetUseCase::toPersonalStatement
        );
        List<String> styleList = List.of("default", "default", "wrap");

        return xlsxGenerator.execute(TEMPLATE_NAME, formList, columnList, styleList, XlsxConstant.FIRST_ROW_INDEX_WITH_NO_TITLE);
    }

    static String toPersonalStatement(Form form) {
        Document document = form.getDocument();
        if (document == null) {
            return "";
        }

        return "1. " + nullToEmpty(document.getLearningExperience()) + ANSWER_SEPARATOR
                + "2. " + nullToEmpty(document.getStatementOfPurpose()) + ANSWER_SEPARATOR
                + "3. " + nullToEmpty(document.getPersonality());
    }

    private static String nullToEmpty(String value) {
        return Objects.requireNonNullElse(value, "");
    }
}
