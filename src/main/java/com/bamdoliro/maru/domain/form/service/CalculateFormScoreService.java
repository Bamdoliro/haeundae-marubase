package com.bamdoliro.maru.domain.form.service;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.value.Attendance;
import com.bamdoliro.maru.domain.form.domain.value.Score;
import com.bamdoliro.maru.domain.form.domain.value.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.bamdoliro.maru.domain.form.constant.FormConstant.*;

@RequiredArgsConstructor
@Service
public class CalculateFormScoreService {

    public void execute(Form form) {
        Double subjectGradeScore = calculateSubjectGradeScore(form);
        Integer attendanceScore = calculateAttendanceScore(form);

        if (form.getEducation().isQualificationExamination()) {
            form.updateScore(new Score(
                    subjectGradeScore,
                    attendanceScore
            ));
        } else {
            Double thirdGradeFirstSemesterSubjectGradeScore = form.getGrade().getSubjectList().getSubjectMap().getScoreOf(3, 1);
            form.updateScore(new Score(
                    subjectGradeScore,
                    thirdGradeFirstSemesterSubjectGradeScore,
                    attendanceScore
            ));
        }
    }

    public Double calculateSubjectGradeScore(Form form) {
        double score = 0;
        if (form.getEducation().isQualificationExamination()) {
            List<Subject> subjectList = form.getGrade().getSubjectList().getValue();
            score += getScore(subjectList);
        }
        else {
            List<Subject> subjectList = form.getGrade().getSubjectList().getSubjectMap().getValue().get("21");
            score += getScore(subjectList, SECOND_GRADE_FIRST_SEMESTER_RATE);

            subjectList = form.getGrade().getSubjectList().getSubjectMap().getValue().get("22");
            score += getScore(subjectList, SECOND_GRADE_SECOND_SEMESTER_RATE);

            subjectList = form.getGrade().getSubjectList().getSubjectMap().getValue().get("31");
            score += getScore(subjectList, THIRD_GRADE_FIRST_SEMESTER_RATE);

            subjectList = form.getGrade().getSubjectList().getSubjectMap().getValue().get("32");
            score += getScore(subjectList, THIRD_GRADE_SECOND_SEMESTER_RATE);
        }

        return score;
    }

    private double getScore(List<Subject> subjectList, double rate) {
        double score = 0;

        for (Subject subject : subjectList) {
            switch(subject.getSubjectName()) {
                case "국어" -> score += subject.getScore() * rate * KOREAN_RATE * 3.5;
                case "수학" -> score += subject.getScore() * rate * MATH_RATE * 3.5;
                case "사회" -> score += subject.getScore() * rate * SOCIAL_RATE * 3.5;
                case "과학" -> score += subject.getScore() * rate * SCIENCE_RATE * 3.5;
                case "영어" -> score += subject.getScore() * rate * ENGLISH_RATE * 3.5;
            }
        }
        return score;
    }

    private double getScore(List<Subject> subjectList) {
        double score = 0;

        for (Subject subject : subjectList) {
            switch(subject.getSubjectName()) {
                case "국어" -> score += subject.getScore() * 1 * KOREAN_RATE * 3.5;
                case "수학" -> score += subject.getScore() * 1 * MATH_RATE * 3.5;
                case "사회" -> score += subject.getScore() * 1 * SOCIAL_RATE * 3.5;
                case "과학" -> score += subject.getScore() * 1 * SCIENCE_RATE * 3.5;
                case "영어" -> score += subject.getScore() * 1 * ENGLISH_RATE * 3.5;
            }
        }
        return score;
    }

    public Double calculateSelfDirectedScoreToRegular(Form form) {
        return form.getScore().getSelfDirectedScore();
    }

    private Integer calculateAttendanceScore(Form form) {
        Attendance totalAttendance = form.getGrade().getTotalAttendance();
        int convertedAbsenceCount = getConvertedAbsenceCount(totalAttendance);
        int penalty;
        if (convertedAbsenceCount <= 0) {
            penalty = 0;
        } else if (convertedAbsenceCount >= 17) {
            penalty = -9;
        } else {
            penalty = -((convertedAbsenceCount - 1) / 2 + 1);
        }
        return penalty;
    }

    private Integer getConvertedAbsenceCount(Attendance attendance) {
        return attendance.getAbsenceCount() + ((attendance.getLatenessCount() + attendance.getEarlyLeaveCount() + attendance.getClassAbsenceCount()) / 3);
    }
}
