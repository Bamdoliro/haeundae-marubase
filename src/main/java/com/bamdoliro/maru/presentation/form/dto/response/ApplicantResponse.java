package com.bamdoliro.maru.presentation.form.dto.response;

import com.bamdoliro.maru.domain.form.domain.type.Gender;
import com.bamdoliro.maru.domain.form.domain.value.Applicant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplicantResponse {

    private String identificationPictureUri;
    private String name;
    private String phoneNumber;
    private String registrationNumber;
    private Gender gender;

    public ApplicantResponse(Applicant applicant, String identificationPictureUri) {
        this.identificationPictureUri = identificationPictureUri;
        this.name = applicant.getName();
        this.phoneNumber = applicant.getPhoneNumber().toString();
        this.registrationNumber = applicant.getRegistrationNumber();
        this.gender = applicant.getGender();
    }
}
