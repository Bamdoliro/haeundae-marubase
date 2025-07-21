package com.bamdoliro.maru.presentation.form.dto.request;

import com.bamdoliro.maru.domain.form.domain.value.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest {

    @NotBlank(message = "필수값입니다.")
    @Size(max = 800, message = "800자 이하여야 합니다.")
    private String learningExperience;

    @NotBlank(message = "필수값입니다.")
    @Size(max = 500, message = "띄어쓰기 제외 300자 이하여야 합니다.")
    private String statementOfPurpose;

    @NotBlank(message = "필수값입니다.")
    @Size(max = 700, message = "띄어쓰기 제외 500자 이하여야 합니다.")
    private String personality;

    public Document toValue() {
        return new Document(
                learningExperience,
                statementOfPurpose,
                personality
        );
    }
}
