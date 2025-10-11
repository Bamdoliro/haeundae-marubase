package com.bamdoliro.maru.presentation.user.dto.response;

import com.bamdoliro.maru.domain.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserWithFormStatusResponse {

    private Long id;
    private String name;
    private String phoneNumber;
    private Boolean hasSubmittedForm;

    public UserWithFormStatusResponse(User user, Boolean hasSubmittedForm) {
        this.id = user.getId();
        this.name = user.getName();
        this.phoneNumber = user.getPhoneNumber();
        this.hasSubmittedForm = hasSubmittedForm;
    }
}