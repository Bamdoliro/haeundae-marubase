package com.bamdoliro.maru.shared.fixture;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.shared.constants.FixedNumber;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SelectFirstPassUseCaseTest, ExportFirstRoundResultUseCaseTest가 쓰는 시드(20261109)로 만든 픽스처가
 * 우연한 중복이나 치우친 분포 없이 쓸 수 있는 데이터인지 확인한다.
 */
class SeededFixtureTest {

    private static final long FIXTURE_SEED = 20261109L;

    @Test
    void 같은_시드로_만들면_같은_원서가_만들어진다() {
        assertThat(snapshot(generate(new Random(FIXTURE_SEED))))
                .isEqualTo(snapshot(generate(new Random(FIXTURE_SEED))));
    }

    @Test
    void 시드로_만든_원서의_주민등록번호와_사용자_전화번호는_겹치지_않는다() {
        Random random = new Random(FIXTURE_SEED);
        List<User> userList = UserFixture.generateUserList(FixedNumber.TOTAL * 2 + 2, random);
        List<Form> formList = FormFixture.generateBusanFormList(userList, random);

        assertThat(formList).extracting(form -> form.getApplicant().getRegistrationNumber()).doesNotHaveDuplicates();
        assertThat(userList).extracting(User::getPhoneNumber).doesNotHaveDuplicates();
    }

    @Test
    void 시드로_만든_원서는_사회통합_정원보다_많은_기회균등_사회다양성_지원자를_포함한다() {
        Map<FormType, Long> countByType = generate(new Random(FIXTURE_SEED)).stream()
                .collect(Collectors.groupingBy(Form::getType, Collectors.counting()));

        long socialIntegrationCount = Math.round(Math.ceil(FixedNumber.SPECIAL * FixedNumber.MULTIPLE));
        assertThat(countByType.get(FormType.ONE_PARENT)).isGreaterThan(socialIntegrationCount / 2);
        assertThat(countByType.get(FormType.MULTI_CHILDREN)).isGreaterThan(socialIntegrationCount / 2);
        assertThat(countByType.get(FormType.REGULAR)).isGreaterThan((long) Math.ceil(FixedNumber.REGULAR * FixedNumber.MULTIPLE));
    }

    private List<Form> generate(Random random) {
        List<User> userList = UserFixture.generateUserList(FixedNumber.TOTAL * 2, random);
        List<Form> formList = FormFixture.generateBusanFormList(userList.subList(0, userList.size() / 2), random);
        formList.addAll(FormFixture.generateOtherRegionFormList(userList.subList(userList.size() / 2, userList.size()), random));
        return formList;
    }

    private List<String> snapshot(List<Form> formList) {
        Function<Form, String> describe = form -> String.join("|",
                form.getApplicant().getRegistrationNumber(),
                form.getType().name(),
                form.getUser().getPhoneNumber(),
                form.getGrade().getSubjectListValue().stream()
                        .map(subject -> String.valueOf(subject.getAchievementLevel()))
                        .collect(Collectors.joining(",")),
                String.valueOf(form.getGrade().getTotalAbsenceCount())
        );
        return formList.stream().map(describe).toList();
    }
}
