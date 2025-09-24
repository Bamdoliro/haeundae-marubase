package com.bamdoliro.maru.domain.form.service;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.AchievementLevel;
import com.bamdoliro.maru.domain.form.domain.value.Attendance;
import com.bamdoliro.maru.domain.form.domain.value.Score;
import com.bamdoliro.maru.domain.form.domain.value.Subject;
import com.bamdoliro.maru.domain.form.domain.value.SubjectMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
            Double thirdGradeFirstSemesterSubjectGradeScore = calculateThirdGradeFirstSemesterScore(form);
            form.updateScore(new Score(
                    subjectGradeScore,
                    thirdGradeFirstSemesterSubjectGradeScore,
                    attendanceScore
            ));
        }
    }

    public Double calculateSubjectGradeScore(Form form) {
        if (form.getEducation().isQualificationExamination()) {
            List<Subject> subjectList = form.getGrade().getSubjectList().getValue();
            return getScore(subjectList);
        }

        SubjectMap originalSubjectMap = form.getGrade().getSubjectList().getSubjectMap();
        SubjectMap replacedSubjectMap = createReplacedSubjectMap(originalSubjectMap);

        double score = 0;
        List<Subject> subjectList;

        subjectList = replacedSubjectMap.getValue().get("21");
        if (subjectList != null) {
            score += getScore(subjectList, SECOND_GRADE_FIRST_SEMESTER_RATE);
        }

        subjectList = replacedSubjectMap.getValue().get("22");
        if (subjectList != null) {
            score += getScore(subjectList, SECOND_GRADE_SECOND_SEMESTER_RATE);
        }

        subjectList = replacedSubjectMap.getValue().get("31");
        if (subjectList != null) {
            score += getScore(subjectList, THIRD_GRADE_FIRST_SEMESTER_RATE);
        }

        subjectList = replacedSubjectMap.getValue().get("32");
        if (subjectList != null) {
            score += getScore(subjectList, THIRD_GRADE_SECOND_SEMESTER_RATE);
        }

        return score;
    }

    private SubjectMap createReplacedSubjectMap(SubjectMap originalSubjectMap) {
        List<Subject> originalSubjects = new ArrayList<>();
        originalSubjectMap.getValue().values().forEach(originalSubjects::addAll);

        List<Subject> replacedSubjects = new ArrayList<>(originalSubjects);
        SubjectMap replacedSubjectMap = new SubjectMap(replacedSubjects);

        replaceSubjectGrade(replacedSubjectMap, "국어");
        replaceSubjectGrade(replacedSubjectMap, "수학");
        replaceSubjectGrade(replacedSubjectMap, "사회");
        replaceSubjectGrade(replacedSubjectMap, "과학");
        replaceSubjectGrade(replacedSubjectMap, "영어");

        return replacedSubjectMap;
    }

    private void replaceSubjectGrade(SubjectMap subjectMap, String subjectName) {
        Subject grade21 = findSubject(subjectMap, subjectName, 2, 1);
        Subject grade22 = findSubject(subjectMap, subjectName, 2, 2);
        Subject grade31 = findSubject(subjectMap, subjectName, 3, 1);
        Subject grade32 = findSubject(subjectMap, subjectName, 3, 2);

        if (isIncomplete(grade21) && !isIncomplete(grade22)) {
            replaceSubject(subjectMap, subjectName, 2, 1, grade22.getAchievementLevel());
        }
        if (isIncomplete(grade22) && !isIncomplete(grade21)) {
            replaceSubject(subjectMap, subjectName, 2, 2, grade21.getAchievementLevel());
        }
        if (isIncomplete(grade31) && !isIncomplete(grade32)) {
            replaceSubject(subjectMap, subjectName, 3, 1, grade32.getAchievementLevel());
        }
        if (isIncomplete(grade32) && !isIncomplete(grade31)) {
            replaceSubject(subjectMap, subjectName, 3, 2, grade31.getAchievementLevel());
        }

        grade21 = findSubject(subjectMap, subjectName, 2, 1);
        grade22 = findSubject(subjectMap, subjectName, 2, 2);
        grade31 = findSubject(subjectMap, subjectName, 3, 1);
        grade32 = findSubject(subjectMap, subjectName, 3, 2);

        if (isIncomplete(grade21) && !isIncomplete(grade31)) {
            replaceSubject(subjectMap, subjectName, 2, 1, grade31.getAchievementLevel());
        }
        if (isIncomplete(grade22) && !isIncomplete(grade32)) {
            replaceSubject(subjectMap, subjectName, 2, 2, grade32.getAchievementLevel());
        }
        if (isIncomplete(grade31) && !isIncomplete(grade21)) {
            replaceSubject(subjectMap, subjectName, 3, 1, grade21.getAchievementLevel());
        }
        if (isIncomplete(grade32) && !isIncomplete(grade22)) {
            replaceSubject(subjectMap, subjectName, 3, 2, grade22.getAchievementLevel());
        }
    }

    private Subject findSubject(SubjectMap subjectMap, String subjectName, int grade, int semester) {
        String key = String.format("%d%d", grade, semester);
        List<Subject> subjects = subjectMap.getValue().get(key);

        if (subjects != null) {
            return subjects.stream()
                    .filter(subject -> subject.getSubjectName().equals(subjectName))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void replaceSubject(SubjectMap subjectMap, String subjectName, int grade, int semester, AchievementLevel newLevel) {
        String key = String.format("%d%d", grade, semester);
        List<Subject> subjects = subjectMap.getValue().get(key);

        if (subjects != null) {
            subjects.replaceAll(subject ->
                    subject.getSubjectName().equals(subjectName)
                            ? new Subject(grade, semester, subjectName, newLevel)
                            : subject
            );
        }
    }

    private boolean isIncomplete(Subject subject) {
        return subject == null || subject.getAchievementLevel() == AchievementLevel.F;
    }

    private Double calculateThirdGradeFirstSemesterScore(Form form) {
        SubjectMap originalSubjectMap = form.getGrade().getSubjectList().getSubjectMap();
        SubjectMap replacedSubjectMap = createReplacedSubjectMap(originalSubjectMap);
        return replacedSubjectMap.getScoreOf(3, 1);
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

        return -convertedAbsenceCount;
    }

    private Integer getConvertedAbsenceCount(Attendance attendance) {
        return attendance.getAbsenceCount() + ((attendance.getLatenessCount() + attendance.getEarlyLeaveCount() + attendance.getClassAbsenceCount()) / 3);
    }
}