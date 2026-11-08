package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.GraduationType;
import com.bamdoliro.maru.domain.form.domain.value.Education;
import com.bamdoliro.maru.domain.form.domain.value.School;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.vo.SchoolStatusVo;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@SpringBootTest
class FormRepositorySchoolStatusTest {

    private static final List<FormStatus> SUBMITTED = List.of(FormStatus.SUBMITTED);

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalculateFormScoreService calculateFormScoreService;

    @BeforeEach
    void setUp() {
        // 소재지, 주소 모두 부산(사상구)
        save("부산사상중", "부산광역시", "부산광역시 사상구 학장로 1", FormStatus.SUBMITTED);
        // 소재지, 주소 모두 부산(해운대구)
        save("부산해운대중", "부산광역시", "부산광역시 해운대구 해운대로 1", FormStatus.SUBMITTED);
        // 소재지만 부산
        save("소재지만부산중", "부산광역시", "경상남도 양산시 물금읍 1", FormStatus.SUBMITTED);
        // 주소만 부산, 앞뒤 공백 포함
        save("주소만부산중", "경상남도", "  부산광역시 사상구 괘법로 1  ", FormStatus.SUBMITTED);
        // 주소 중간에 우연히 "부산광역시"가 들어간 타지역
        save("서울부산로중", "서울특별시", "서울특별시 강남구 부산광역시로 1", FormStatus.SUBMITTED);
        // 타지역
        save("김해중", "경상남도", "경상남도 김해시 김해대로 1", FormStatus.SUBMITTED);
        // 주소, 소재지 모두 없음
        save("지역없음중", null, null, FormStatus.SUBMITTED);
        save("지역공백중", "  ", "", FormStatus.SUBMITTED);
        // 요청하지 않은 상태의 원서
        save("승인부산중", "부산광역시", "부산광역시 사상구 학장로 2", FormStatus.APPROVED);
        save("승인김해중", "경상남도", "경상남도 김해시 김해대로 2", FormStatus.APPROVED);
    }

    @Test
    void 부산_전체는_주소나_소재지_중_하나라도_부산인_원서를_조회한다() {
        assertThat(schoolNames(formRepository.findSchoolByAddress(SUBMITTED, "부산광역시")))
                .containsExactlyInAnyOrder("부산사상중", "부산해운대중", "소재지만부산중", "주소만부산중");
    }

    @Test
    void 구를_지정하면_해당_구의_원서만_조회한다() {
        assertThat(schoolNames(formRepository.findSchoolByAddress(SUBMITTED, "부산광역시 사상구")))
                .containsExactlyInAnyOrder("부산사상중", "주소만부산중");
    }

    @Test
    void 구_이름의_일부만_일치하면_조회하지_않는다() {
        assertThat(formRepository.findSchoolByAddress(SUBMITTED, "부산광역시 사상")).isEmpty();
    }

    @Test
    void 타지역은_부산으로_판정된_원서와_지역이_없는_원서를_제외한다() {
        assertThat(schoolNames(formRepository.findNotBusanSchool(SUBMITTED)))
                .containsExactlyInAnyOrder("서울부산로중", "김해중");
    }

    @Test
    void 부산과_타지역_조회_결과는_서로_겹치지_않는다() {
        Set<String> busan = new HashSet<>(schoolNames(formRepository.findSchoolByAddress(SUBMITTED, "부산광역시")));
        Set<String> notBusan = new HashSet<>(schoolNames(formRepository.findNotBusanSchool(SUBMITTED)));

        assertThat(busan).doesNotContainAnyElementsOf(notBusan);
        assertThat(busan.size() + notBusan.size()).isEqualTo(6);
    }

    @Test
    void 주소와_소재지가_모두_없는_원서는_양쪽_결과에서_제외한다() {
        assertThat(schoolNames(formRepository.findSchoolByAddress(SUBMITTED, "부산광역시")))
                .doesNotContain("지역없음중", "지역공백중");
        assertThat(schoolNames(formRepository.findNotBusanSchool(SUBMITTED)))
                .doesNotContain("지역없음중", "지역공백중");
    }

    @Test
    void 타지역_조회에도_원서_상태_조건이_적용된다() {
        assertThat(schoolNames(formRepository.findNotBusanSchool(SUBMITTED)))
                .doesNotContain("승인김해중");
        assertThat(schoolNames(formRepository.findNotBusanSchool(List.of(FormStatus.APPROVED))))
                .containsExactly("승인김해중");
    }

    @Test
    void 부산_조회에도_원서_상태_조건이_적용된다() {
        assertThat(schoolNames(formRepository.findSchoolByAddress(List.of(FormStatus.APPROVED), "부산광역시")))
                .containsExactly("승인부산중");
    }

    private void save(String schoolName, String location, String address, FormStatus status) {
        User user = userRepository.save(UserFixture.createUser());
        Form form = FormFixture.createRandomBusanForm(user);
        form.update(
                form.getApplicant(),
                form.getParent(),
                new Education(
                        GraduationType.EXPECTED,
                        form.getEducation().getGraduationDate(),
                        new School(schoolName, location, address, "7150000"),
                        form.getEducation().getTeacher()
                ),
                form.getGrade(),
                form.getDocument(),
                form.getType()
        );
        calculateFormScoreService.execute(form);
        if (status == FormStatus.APPROVED) {
            form.approve();
        }
        formRepository.save(form);
    }

    private List<String> schoolNames(List<SchoolStatusVo> result) {
        return result.stream()
                .map(SchoolStatusVo::getSchoolName)
                .toList();
    }
}
