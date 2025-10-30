package com.bamdoliro.maru.domain.form.domain.value;

import com.bamdoliro.maru.shared.util.MathUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class Score {

    @Column(nullable = false)
    private Double subjectGradeScore;

    @Column(nullable = true)
    private Double thirdGradeFirstSemesterSubjectGradeScore;

    @Column(nullable = true)
    private Double thirdGradeSecondSemesterSubjectGradeScore;

    @Column(nullable = true)
    private Double secondGradeSecondSemesterSubjectGradeScore;

    @Column(nullable = true)
    private Double secondGradeFirstSemesterSubjectGradeScore;

    @Column(nullable = false)
    private Integer attendanceScore;

    @Column(nullable = true)
    private Double selfDirectedScore;

    @Column(nullable = true)
    private Double personalityScore;

    @Column(nullable = false)
    private Double firstRoundScore;

    @Column(nullable = true)
    private Double totalScore;

    public Score(Double subjectGradeScore,
                 Double thirdGradeFirstSemesterSubjectGradeScore,
                 Double thirdGradeSecondSemesterSubjectGradeScore,
                 Double secondGradeSecondSemesterSubjectGradeScore,
                 Double secondGradeFirstSemesterSubjectGradeScore,
                 Integer attendanceScore) {
        this.subjectGradeScore = MathUtil.roundTo(subjectGradeScore, 3);
        this.thirdGradeFirstSemesterSubjectGradeScore = MathUtil.roundTo(thirdGradeFirstSemesterSubjectGradeScore, 3);
        this.thirdGradeSecondSemesterSubjectGradeScore = MathUtil.roundTo(thirdGradeSecondSemesterSubjectGradeScore, 3);
        this.secondGradeSecondSemesterSubjectGradeScore = MathUtil.roundTo(secondGradeSecondSemesterSubjectGradeScore, 3);
        this.secondGradeFirstSemesterSubjectGradeScore = MathUtil.roundTo(secondGradeFirstSemesterSubjectGradeScore, 3);
        this.attendanceScore = attendanceScore;
        this.firstRoundScore = MathUtil.roundTo(subjectGradeScore + attendanceScore, 3);
    }

    public Score(Double subjectGradeScore, Integer attendanceScore) {
        this.subjectGradeScore = MathUtil.roundTo(subjectGradeScore, 3);
        this.attendanceScore = attendanceScore;
        this.firstRoundScore = MathUtil.roundTo(subjectGradeScore + attendanceScore , 3);
    }

    public void updateSubjectScore(Double subjectGradeScore) {
        this.subjectGradeScore = subjectGradeScore;
        this.firstRoundScore = subjectGradeScore + attendanceScore;
    }

    public void updateSecondRoundMeisterScore(Double selfDirectedScore, Double personalityScore) {
        this.selfDirectedScore = selfDirectedScore;
        this.personalityScore = personalityScore;
        this.totalScore = firstRoundScore + selfDirectedScore + personalityScore;
    }

    public void updateSecondRoundScore(Double selfDirectedScore, Double personalityScore) {
        this.selfDirectedScore = selfDirectedScore;
        this.personalityScore = personalityScore;
        this.totalScore = firstRoundScore + selfDirectedScore + personalityScore;
    }

    public void updateSecondRoundSocialScoreToRegular(Double selfDirectedScore) {
        this.selfDirectedScore = selfDirectedScore;
        this.totalScore = firstRoundScore + selfDirectedScore + personalityScore;
    }
}
