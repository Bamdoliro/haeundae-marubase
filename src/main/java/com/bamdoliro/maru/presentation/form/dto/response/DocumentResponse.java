package com.bamdoliro.maru.presentation.form.dto.response;

import com.bamdoliro.maru.domain.form.domain.value.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentResponse {

    private String learningExperience;
    private String statementOfPurpose;
    private String personality;

    public DocumentResponse(Document document) {
        this.learningExperience = document.getLearningExperience();
        this.statementOfPurpose = document.getStatementOfPurpose();
        this.personality = document.getPersonality();
    }
}
