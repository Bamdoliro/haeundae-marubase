package com.bamdoliro.maru.presentation.form.dto.request;

import com.bamdoliro.maru.domain.form.domain.type.Gender;
import com.bamdoliro.maru.domain.form.domain.value.Applicant;
import com.bamdoliro.maru.domain.form.domain.value.PhoneNumber;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantRequest {

    @NotBlank(message = "필수값입니다.")
    @Size(max = 20, message = "20자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "필수값입니다.")
    @Size(min = 11, max = 11, message = "11자여야 합니다.")
    private String phoneNumber;

    @NotNull(message = "필수값입니다.")
    @Past(message = "과거여야 합니다.")
    private LocalDate birthday;

    @NotBlank(message = "필수값입니다.")
    @Size(min = 14, max = 14, message = "14자여야 합니다.")
    private String registrationNumber;

    @NotNull(message = "필수값입니다.")
    private Gender gender;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "필수값입니다.")
    private String email;

    public Applicant toValue() {
        System.out.println("sex: " + registrationNumber.length());
        return new Applicant(
                name,
                new PhoneNumber(phoneNumber),
                birthday,
                registrationNumber,
                gender,
                email
        );
    }
}
