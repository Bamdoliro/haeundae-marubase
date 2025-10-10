package com.bamdoliro.maru.domain.form.domain.value;

import com.bamdoliro.maru.domain.form.domain.type.Gender;
import com.bamdoliro.maru.infrastructure.persistence.converter.LocalDateEncryptedConverter;
import com.bamdoliro.maru.infrastructure.persistence.converter.StringEncryptedConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class Applicant {

    @Convert(converter = StringEncryptedConverter.class)
    @Column(nullable = false)
    private String name;


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "phone_number", nullable = false)),
    })
    private PhoneNumber phoneNumber;


    @Convert(converter = LocalDateEncryptedConverter.class)
    @Column(nullable = false)
    private LocalDate birthday;

    @Convert(converter = StringEncryptedConverter.class)
    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Gender gender;
}
