package com.bamdoliro.maru.domain.form.service;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.ExaminationNumberExhaustedException;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AssignExaminationNumberService {

    /**
     * 전형별 수험번호 구간 크기. 시작값과 시작값 + RANGE_SIZE는 구간에 포함하지 않는다.
     * (예: 일반전형 111000 -> 111001 ~ 111999)
     */
    private static final long RANGE_SIZE = 1000L;

    private final FormRepository formRepository;
    private final ExaminationNumberLock examinationNumberLock;

    public void execute(Form form) {
        examinationNumberLock.lock();
        Long examinationNumber = getNextExaminationNumber(form.getType());
        form.assignExaminationNumber(examinationNumber);
    }

    private Long getNextExaminationNumber(FormType type) {
        long startNumber = getStartNumber(type);
        long endNumber = startNumber + RANGE_SIZE;

        long nextNumber = formRepository.findAllExaminationNumber().stream()
                .filter(num -> num != null && num > startNumber && num < endNumber)
                .max(Long::compareTo)
                .orElse(startNumber) + 1;

        if (nextNumber >= endNumber) {
            throw new ExaminationNumberExhaustedException();
        }

        return nextNumber;
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
