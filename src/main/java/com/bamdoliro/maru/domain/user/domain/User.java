package com.bamdoliro.maru.domain.user.domain;

import com.bamdoliro.maru.domain.user.domain.type.Authority;
import com.bamdoliro.maru.domain.user.domain.value.Password;
import com.bamdoliro.maru.infrastructure.persistence.converter.StringEncryptedConverter;
import com.bamdoliro.maru.shared.util.PasswordUtil;
import com.bamdoliro.maru.shared.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_user")
@Entity
public class User extends BaseTimeEntity {

    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @Convert(converter = StringEncryptedConverter.class)
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Convert(converter = StringEncryptedConverter.class)
    @Column(nullable = false)
    private String name;

    @Embedded
    private Password password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Authority authority;

    /**
     * 관리자가 원서 접수 테스트용으로 생성한 유저인지 여부. 실제 지원자는 항상 false.
     * ddl-auto: update로 기존 행이 있는 테이블에 NOT NULL 컬럼을 추가하려면 DB 기본값이 필요해 columnDefinition을 둔다.
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isTestData;

    public User(String phoneNumber, String name, String password, Authority authority) {
        this(phoneNumber, name, password, authority, false);
    }

    @Builder
    public User(String phoneNumber, String name, String password, Authority authority, boolean isTestData) {
        this(phoneNumber, name, new Password(PasswordUtil.encode(password)), authority, isTestData);
    }

    private User(String phoneNumber, String name, Password password, Authority authority, boolean isTestData) {
        this.uuid = UUID.randomUUID();
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.password = password;
        this.authority = authority;
        this.isTestData = isTestData;
    }

    /**
     * 테스트 원서용 유저를 만든다. 대량 생성 시 BCrypt 인코딩을 유저마다 반복하지 않도록 미리 인코딩한 비밀번호를 받는다.
     */
    public static User createTestData(String phoneNumber, String name, Password encodedPassword) {
        return new User(phoneNumber, name, encodedPassword, Authority.USER, true);
    }

    public boolean isAdmin() {
        return this.authority == Authority.ADMIN;
    }

    public void updatePassword(String password) {
        this.password = new Password(PasswordUtil.encode(password));
    }
}
