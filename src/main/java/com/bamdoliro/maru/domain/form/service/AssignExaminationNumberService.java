package com.bamdoliro.maru.domain.form.service;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AssignExaminationNumberService {

    private final FormRepository formRepository;

    public void execute(Form form) {
        Long examinationNumber = getNextExaminationNumber(form.getType());
        form.assignExaminationNumber(examinationNumber);
    }

    private Long getNextExaminationNumber(FormType type) {
        Long startNumber = getStartNumber(type);
        return formRepository.findAllExaminationNumber().stream()
                .filter(num -> num >= startNumber && num <= startNumber + 1000)
                .max(Long::compareTo)
                .orElse(startNumber) + 1;
    }

    private Long getStartNumber(FormType type) {
        return switch (type.getSubCategory()) {
            case REGULAR -> 111000L;
            case EQUAL_OPPORTUNITY -> 212000L;
            case SOCIETY_DIVERSITY -> 213000L;
            case SPECIAL_ADMISSION -> 314000L;
            case NATIONAL_VETERANS_EDUCATION -> 415000L;
            default -> 0L;
        };
    }
}
