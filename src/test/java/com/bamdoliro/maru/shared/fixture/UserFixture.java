package com.bamdoliro.maru.shared.fixture;

import com.bamdoliro.maru.domain.user.domain.SignUpVerification;
import com.bamdoliro.maru.domain.user.domain.UpdatePasswordVerification;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.user.domain.type.Authority;
import com.bamdoliro.maru.shared.util.RandomCodeUtil;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.bamdoliro.maru.shared.util.RandomUtil.randomPhoneNumber;

public class UserFixture {

    public static User createUser() {
        return new User(randomPhoneNumber(), "김밤돌", "비밀번호", Authority.USER);
    }

    public static User createUserWithId(Long id) {
        User user = new User(randomPhoneNumber(), "김밤돌", "비밀번호", Authority.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static User createAdminUser() {
        return new User("01085852525", "김드민", "비밀번호", Authority.ADMIN);
    }

    public static SignUpVerification createSignUpVerification(boolean verified) {
        SignUpVerification signUpVerification = new SignUpVerification("01085852525");
        if (verified) {
            signUpVerification.verify();
        }

        return signUpVerification;
    }

    public static UpdatePasswordVerification createUpdatePasswordVerification(boolean verified) {
        UpdatePasswordVerification updatePasswordVerification = new UpdatePasswordVerification("01085852525");
        if (verified) {
            updatePasswordVerification.verify();
        }

        return updatePasswordVerification;
    }

    public static List<User> generateUserList(int size) {
        Set<String> usedPhoneNumbers = new HashSet<>();
        return IntStream.range(0, size)
                .mapToObj(i -> new User(generateUniquePhoneNumber(() -> RandomCodeUtil.generate(11), usedPhoneNumbers), "김밤돌" + i, "비밀번호", Authority.USER))
                .toList();
    }

    /**
     * 시드를 고정한 Random을 넘기면 같은 사용자 목록이 재현된다.
     */
    public static List<User> generateUserList(int size, Random random) {
        Set<String> usedPhoneNumbers = new HashSet<>();
        return IntStream.range(0, size)
                .mapToObj(i -> new User(generateUniquePhoneNumber(() -> randomDigits(random, 11), usedPhoneNumbers), "김밤돌" + i, "비밀번호", Authority.USER))
                .toList();
    }

    /**
     * 같은 목록 안에서 이미 쓴 전화번호가 나오면 다시 만든다. 사용자 저장 시 전화번호 unique 제약에 걸리지 않도록 한다.
     */
    private static String generateUniquePhoneNumber(Supplier<String> generator, Set<String> used) {
        String phoneNumber;
        do {
            phoneNumber = generator.get();
        } while (!used.add(phoneNumber));
        return phoneNumber;
    }

    private static String randomDigits(Random random, int count) {
        StringBuilder digits = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            digits.append(random.nextInt(10));
        }
        return digits.toString();
    }
}
