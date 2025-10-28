package com.bamdoliro.maru.domain.form.domain.value;

import com.bamdoliro.maru.domain.form.domain.type.GraduationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class Education {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private GraduationType graduationType;

    @Column(nullable = false, length = 10)
    private String graduationDate;

    @Embedded
    private School school;

    @Embedded
    private Teacher teacher;

    public boolean isQualificationExamination() {
        return graduationType.equals(GraduationType.QUALIFICATION_EXAMINATION);
    }

    public boolean isGraduated() {
        return graduationType.equals(GraduationType.GRADUATED);
    }

    public boolean isExpected() {
        return graduationType.equals(GraduationType.EXPECTED);
    }

    public String getGraduationTypeToString() {
        String[] dateParts = graduationDate.split("-");
        return String.format("%s년 %s월 %s일 %s", dateParts[0], dateParts[1], dateParts[2], graduationType.getDescription());
    }
}
