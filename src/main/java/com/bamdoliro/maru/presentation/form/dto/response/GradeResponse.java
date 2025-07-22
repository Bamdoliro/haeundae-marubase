package com.bamdoliro.maru.presentation.form.dto.response;

import com.bamdoliro.maru.domain.form.domain.type.Certificate;
import com.bamdoliro.maru.domain.form.domain.value.Grade;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GradeResponse {

    private List<SubjectResponse> subjectList;
    private AttendanceResponse attendance1;
    private AttendanceResponse attendance2;
    private AttendanceResponse attendance3;

    public GradeResponse(Grade grade) {
        this.subjectList = grade.getSubjectList().getValue()
                .stream()
                .map(SubjectResponse::new)
                .toList();
        this.attendance1 = new AttendanceResponse(grade.getAttendance1());
        this.attendance2 = new AttendanceResponse(grade.getAttendance2());
        this.attendance3 = new AttendanceResponse(grade.getAttendance3());
    }
}
