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
        return switch (type.getCategory()) {
            case REGULAR -> 1000L;
            case SPECIAL -> 2000L;
            case SUPERNUMERARY -> 3000L;
            default -> 0L;
        };
    }
}
