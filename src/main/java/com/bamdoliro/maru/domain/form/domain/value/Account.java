package com.bamdoliro.maru.domain.form.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class Account {

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String bank;
}
