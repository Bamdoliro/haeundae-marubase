package com.bamdoliro.maru.application.fair;

import com.bamdoliro.maru.domain.fair.domain.Fair;
import com.bamdoliro.maru.infrastructure.message.SendMessageService;
import com.bamdoliro.maru.infrastructure.persistence.fair.FairRepository;
import com.bamdoliro.maru.presentation.fair.dto.request.AttendAdmissionFairRequest;
import com.bamdoliro.maru.shared.fixture.FairFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@SpringBootTest
class AttendAdmissionFairUseCaseSpringBootTest {

    @Autowired
    private AttendAdmissionFairUseCase attendAdmissionFairUseCase;

    @Autowired
    private FairRepository fairRepository;

    // 실제 문자(솔라피)가 발송되지 않도록 대체한다.
    @MockBean
    private SendMessageService sendMessageService;

    @Test
    void 입학설명회_참석_신청을_한다() {
        Fair fair = fairRepository.save(FairFixture.createFair());
        AttendAdmissionFairRequest request = new AttendAdmissionFairRequest(
                "비전중학교",
                "김한울",
                "학생",
                "01012345678",
                2,
                3,
                "급식 존맛?"
        );

        attendAdmissionFairUseCase.execute(fair.getId(), request);

        verify(sendMessageService, times(1)).execute(eq("01012345678"), anyString(), anyString());
    }
}