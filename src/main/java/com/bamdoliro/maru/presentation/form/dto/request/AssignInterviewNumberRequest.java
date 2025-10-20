package com.bamdoliro.maru.presentation.form.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssignInterviewNumberRequest {

    @NotNull(message = "면접번호는 필수입니다.")
    private Long interviewNumber;
}