package com.bamdoliro.maru.presentation.fair.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAttendeeListRequest {

    @NotEmpty(message = "필수값입니다.")
    private List<@Valid DeleteAttendeeRequest> attendeeList;
}
