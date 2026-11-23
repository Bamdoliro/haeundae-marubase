package com.bamdoliro.maru.shared.fixture;

import com.bamdoliro.maru.domain.user.domain.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class UserFixtureTest {

    @Test
    void 같은_목록_안에서_전화번호가_겹치면_다시_만든다() {
        // 처음 두 번은 00000000000, 그 뒤로는 11111111111을 만드는 Random
        Random random = new Random() {
            private int calls = 0;

            @Override
            public int nextInt(int bound) {
                return calls++ < 22 ? 0 : 1;
            }
        };

        List<User> userList = UserFixture.generateUserList(2, random);

        assertThat(userList).extracting(User::getPhoneNumber)
                .containsExactly("00000000000", "11111111111");
    }

    @Test
    void 시드_없이_많이_만들어도_전화번호가_겹치지_않는다() {
        List<User> userList = UserFixture.generateUserList(1_000);

        assertThat(userList).extracting(User::getPhoneNumber).doesNotHaveDuplicates();
    }
}
