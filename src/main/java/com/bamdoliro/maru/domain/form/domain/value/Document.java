package com.bamdoliro.maru.domain.form.domain.value;

import com.bamdoliro.maru.infrastructure.persistence.converter.StringEncryptedConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class Document {

    @Convert(converter = StringEncryptedConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String learningExperience;
    ;

    @Convert(converter = StringEncryptedConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String statementOfPurpose;

    @Convert(converter = StringEncryptedConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String personality;
}
