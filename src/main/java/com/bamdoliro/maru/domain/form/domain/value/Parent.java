package com.bamdoliro.maru.domain.form.domain.value;

import com.bamdoliro.maru.infrastructure.persistence.converter.StringEncryptedConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class Parent {

    @Convert(converter = StringEncryptedConverter.class)
    @Column(name = "parent_name", nullable = false)
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "parent_phone_number", nullable = false)),
    })
    private PhoneNumber phoneNumber;

    @Column(name = "parent_relation", nullable = false, length = 20)
    private String relation;

    @Embedded
    private Address address;

    @Column(nullable = false)
    private String account;
}
