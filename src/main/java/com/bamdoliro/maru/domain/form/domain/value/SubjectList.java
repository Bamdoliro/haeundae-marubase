package com.bamdoliro.maru.domain.form.domain.value;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class SubjectList {

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tbl_subject",
            joinColumns = @JoinColumn(name = "form_id"))
    private List<Subject> value;

    public static SubjectList of(List<Subject> subjectList) {
        return new SubjectList(subjectList == null ? List.of() : subjectList);
    }

    public SubjectMap getSubjectMap() {
        return new SubjectMap(value);
    }

    /**
     * 학년/학기별 과목. 검정고시 과목은 학기 구분 없이 학년/학기 0으로 저장되므로,
     * 검정고시 과목이 있으면 어느 학기를 물어도 검정고시 과목 전체를 돌려준다.
     * 검정고시 점수는 학기 구분 없이 과목 전체로 계산하므로, 학기별 칸에도 같은 성적을 채운다.
     */
    public SubjectList getSubjectListForSemester(int grade, int semester) {
        SubjectMap subjectMap = getSubjectMap();
        SubjectList qualificationExaminationSubjects = subjectMap.getSubjectListOf(0, 0);
        if (qualificationExaminationSubjects.size() > 0) {
            return qualificationExaminationSubjects;
        }
        return subjectMap.getSubjectListOf(grade, semester);
    }

    public Double getAverageScore() {
        return (double) totalScore() / (double) size();
    }

    public Integer totalScore() {
        return value.stream()
                .mapToInt(Subject::getScore)
                .sum();
    }

    public Integer size() {
        return value.size();
    }
}
