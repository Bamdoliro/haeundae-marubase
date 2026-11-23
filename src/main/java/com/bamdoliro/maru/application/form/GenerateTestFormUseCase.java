package com.bamdoliro.maru.application.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.exception.TestDataUniqueValueExhaustedException;
import com.bamdoliro.maru.domain.form.service.AssignExaminationNumberService;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.form.service.RandomFormGenerator;
import com.bamdoliro.maru.domain.form.service.RandomFormGenerator.TestApplicant;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.user.domain.value.Password;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.presentation.form.dto.request.GenerateTestFormRequest;
import com.bamdoliro.maru.presentation.form.dto.request.TestFormCountRequest;
import com.bamdoliro.maru.presentation.form.dto.response.GenerateTestFormResponse;
import com.bamdoliro.maru.presentation.form.dto.response.TestFormCountResponse;
import com.bamdoliro.maru.shared.annotation.UseCase;
import com.bamdoliro.maru.shared.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 원서 접수 테스트용 원서를 전형별로 지정한 개수만큼 만든다.
 * 회원가입(SignUpUserUseCase)을 거치지 않고 유저를 바로 저장하므로 인증 문자 등 SMS는 보내지 않는다.
 * 원서는 실제 흐름과 같이 최종 제출, 전형료 납부, 확인 중(RECEIVED)을 거쳐 승인(APPROVED) 상태로 만들고, 수험번호는 실제 제출과 같은 AssignExaminationNumberService로 전형별 구간 안에서 받는다.
 */
@RequiredArgsConstructor
@UseCase
public class GenerateTestFormUseCase {

    static final int MAX_UNIQUE_VALUE_ATTEMPTS = 20;

    private final UserRepository userRepository;
    private final FormRepository formRepository;
    private final RandomFormGenerator randomFormGenerator;
    private final CalculateFormScoreService calculateFormScoreService;
    private final AssignExaminationNumberService assignExaminationNumberService;

    @Transactional
    public GenerateTestFormResponse execute(GenerateTestFormRequest request) {
        // 테스트 유저는 로그인할 일이 없으므로 아무도 모르는 비밀번호 하나를 한 번만 인코딩해 같이 쓴다.
        Password password = new Password(PasswordUtil.encode(UUID.randomUUID().toString()));
        Set<String> usedPhoneNumbers = new HashSet<>();
        Set<String> usedRegistrationNumbers = new HashSet<>();
        Map<FormType, Integer> countByType = new LinkedHashMap<>();

        for (TestFormCountRequest item : request.getFormList()) {
            for (int i = 0; i < item.getCount(); i++) {
                generate(item.getType(), password, usedPhoneNumbers, usedRegistrationNumbers);
                countByType.merge(item.getType(), 1, Integer::sum);
            }
        }

        List<TestFormCountResponse> formList = countByType.entrySet().stream()
                .map(entry -> new TestFormCountResponse(entry.getKey(), entry.getValue()))
                .toList();
        int totalCount = formList.stream().mapToInt(TestFormCountResponse::getCount).sum();

        return new GenerateTestFormResponse(totalCount, formList);
    }

    private void generate(FormType type, Password password, Set<String> usedPhoneNumbers, Set<String> usedRegistrationNumbers) {
        TestApplicant applicant = randomFormGenerator.generateApplicant();
        String phoneNumber = generateUnique(
                randomFormGenerator::generatePhoneNumber,
                userRepository::existsByPhoneNumber,
                usedPhoneNumbers
        );
        String registrationNumber = generateUnique(
                () -> randomFormGenerator.generateRegistrationNumber(applicant),
                formRepository::existsByApplicant_RegistrationNumber,
                usedRegistrationNumbers
        );

        User user = userRepository.save(User.createTestData(phoneNumber, applicant.name(), password));

        Form form = randomFormGenerator.generate(user, type, applicant, phoneNumber, registrationNumber);
        calculateFormScoreService.execute(form);
        assignExaminationNumberService.execute(form);
        form.submit();
        form.payFee();
        form.receive();
        form.approve();
        formRepository.save(form);
    }

    /**
     * DB와 이번 요청에서 이미 만든 값 모두와 겹치지 않는 값을 만든다. 유니크 제약 위반으로 전체가 롤백되지 않도록 저장 전에 확인한다.
     */
    private String generateUnique(Supplier<String> generator, Predicate<String> existsInDatabase, Set<String> used) {
        for (int attempt = 0; attempt < MAX_UNIQUE_VALUE_ATTEMPTS; attempt++) {
            String value = generator.get();
            if (!used.contains(value) && !existsInDatabase.test(value)) {
                used.add(value);
                return value;
            }
        }
        throw new TestDataUniqueValueExhaustedException();
    }
}
