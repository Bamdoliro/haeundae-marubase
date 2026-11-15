package com.bamdoliro.maru.presentation.form.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GenerateTestFormResponse {

    private Integer totalCount;
    private List<TestFormCountResponse> formList;
}
