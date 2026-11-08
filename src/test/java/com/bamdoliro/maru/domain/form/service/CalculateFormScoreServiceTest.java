package com.bamdoliro.maru.domain.form.service;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.value.Attendance;
import com.bamdoliro.maru.domain.form.domain.value.Grade;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class CalculateFormScoreServiceTest {

    @InjectMocks
    private CalculateFormScoreService calculateFormScoreService;

    @Test
    void 일반전형_1차_점수를_계산한다() {
        // given
        Form form = FormFixture.createForm(FormType.REGULAR);

        // when
        calculateFormScoreService.execute(form);

        // then
        assertEquals(135.296, form.getScore().getSubjectGradeScore());
        assertEquals(38.4, form.getScore().getThirdGradeFirstSemesterSubjectGradeScore());
        assertEquals(-3, form.getScore().getAttendanceScore());
    }

    @Test
    void 특별전형_1차_점수를_계산한다() {
        // given
        Form form = FormFixture.createForm(FormType.NATIONAL_VETERANS);

        // when
        calculateFormScoreService.execute(form);

        // then
        assertEquals(135.296, form.getScore().getSubjectGradeScore());
        assertEquals(38.4, form.getScore().getThirdGradeFirstSemesterSubjectGradeScore());
        assertEquals(-3, form.getScore().getAttendanceScore());
    }

    @Test
    void 검정고시_일반전형_1차_점수를_계산한다() {
        // given
        Form form = FormFixture.createQualificationExaminationForm(FormType.REGULAR);

        // when
        calculateFormScoreService.execute(form);

        // then
        assertEquals(100.24, form.getScore().getSubjectGradeScore());
        assertNull(form.getScore().getThirdGradeFirstSemesterSubjectGradeScore());
        assertEquals(0, form.getScore().getAttendanceScore());
    }

    @Test
    void 검정고시_특별전형_1차_점수를_계산한다() {
        // given
        Form form = FormFixture.createQualificationExaminationForm(FormType.NATIONAL_VETERANS);

        // when
        calculateFormScoreService.execute(form);

        // then
        assertEquals(100.24, form.getScore().getSubjectGradeScore());
        assertEquals(0, form.getScore().getAttendanceScore());
    }

    @Test
    void 검정고시_전형은_출결_입력값과_무관하게_출결_점수가_0점이다() {
        // given
        Form form = withAttendance(
                FormFixture.createQualificationExaminationForm(FormType.REGULAR),
                new Attendance(10, 3, 3, 3),
                new Attendance(5, 0, 0, 0),
                new Attendance(0, 0, 0, 0)
        );

        // when
        calculateFormScoreService.execute(form);

        // then
        assertEquals(0, form.getScore().getAttendanceScore());
        assertEquals(100.24, form.getScore().getFirstRoundScore());
    }

    @Test
    void 일반전형은_환산_결석일수만큼_감점한다() {
        // given
        Form form = withAttendance(
                FormFixture.createForm(FormType.REGULAR),
                new Attendance(2, 0, 0, 0),
                new Attendance(1, 0, 0, 0),
                new Attendance(0, 0, 0, 0)
        );

        // when
        calculateFormScoreService.execute(form);

        // then
        assertEquals(-3, form.getScore().getAttendanceScore());
    }

    @Test
    void 일반전형은_결석일수가_많아도_상한_없이_선형으로_감점한다() {
        // given
        Form twentyDays = withAttendance(
                FormFixture.createForm(FormType.REGULAR),
                new Attendance(10, 0, 0, 0),
                new Attendance(10, 0, 0, 0),
                new Attendance(0, 0, 0, 0)
        );
        Form thirtyDays = withAttendance(
                FormFixture.createForm(FormType.REGULAR),
                new Attendance(10, 0, 0, 0),
                new Attendance(10, 0, 0, 0),
                new Attendance(10, 0, 0, 0)
        );

        // when
        calculateFormScoreService.execute(twentyDays);
        calculateFormScoreService.execute(thirtyDays);

        // then
        assertEquals(-20, twentyDays.getScore().getAttendanceScore());
        assertEquals(-30, thirtyDays.getScore().getAttendanceScore());
    }

    @Test
    void 지각_조퇴_결과는_합산_3회를_결석_1일로_환산하고_나머지는_버린다() {
        // given
        Form threeTimes = withAttendance(
                FormFixture.createForm(FormType.REGULAR),
                new Attendance(0, 1, 0, 0),
                new Attendance(0, 0, 1, 0),
                new Attendance(0, 0, 0, 1)
        );
        Form eightTimes = withAttendance(
                FormFixture.createForm(FormType.REGULAR),
                new Attendance(1, 3, 0, 0),
                new Attendance(0, 0, 3, 0),
                new Attendance(0, 0, 0, 2)
        );

        // when
        calculateFormScoreService.execute(threeTimes);
        calculateFormScoreService.execute(eightTimes);

        // then
        assertEquals(-1, threeTimes.getScore().getAttendanceScore());
        assertEquals(-3, eightTimes.getScore().getAttendanceScore());
    }

    private Form withAttendance(Form form, Attendance attendance1, Attendance attendance2, Attendance attendance3) {
        form.update(
                form.getApplicant(),
                form.getParent(),
                form.getEducation(),
                new Grade(form.getGrade().getSubjectList(), attendance1, attendance2, attendance3),
                form.getDocument(),
                form.getType()
        );
        return form;
    }
}
