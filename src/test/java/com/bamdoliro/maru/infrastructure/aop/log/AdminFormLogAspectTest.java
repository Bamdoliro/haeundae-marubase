package com.bamdoliro.maru.infrastructure.aop.log;

import com.bamdoliro.maru.domain.auth.service.TokenService;
import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.service.AssignExaminationNumberService;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.log.AdminFormStatusChangeLog;
import com.bamdoliro.maru.domain.log.AdminFromViewLog;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminFormStatusChangeLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.log.AdminFormViewLogRepository;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import com.bamdoliro.maru.infrastructure.s3.FileService;
import com.bamdoliro.maru.shared.config.DatabaseClearExtension;
import com.bamdoliro.maru.shared.fixture.FormFixture;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 요청(MockMvc)과 DB로 관리자 열람 로그, 상태 변경 로그가 쌓이는지 확인한다.
 */
@ActiveProfiles("test")
@ExtendWith(DatabaseClearExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
class AdminFormLogAspectTest {

    private static final String USER_AGENT = "admin-browser";
    private static final String CLIENT_IP = "203.0.113.10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private AssignExaminationNumberService assignExaminationNumberService;

    @Autowired
    private CalculateFormScoreService calculateFormScoreService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AdminFormViewLogRepository adminFormViewLogRepository;

    @Autowired
    private AdminFormStatusChangeLogRepository adminFormStatusChangeLogRepository;

    // 원서 조회 시 S3 presigned URL 발급은 이 테스트의 관심사가 아니다.
    @MockBean
    private FileService fileService;

    private User admin;
    private User applicant;
    private Form form;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(UserFixture.createAdminUser());
        applicant = userRepository.save(UserFixture.createUser());

        form = FormFixture.createRandomBusanForm(applicant);
        assignExaminationNumberService.execute(form);
        calculateFormScoreService.execute(form);
        form.submit();
        form = formRepository.save(form);
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.generateAccessToken(user.getPhoneNumber());
    }

    @Test
    void 관리자가_원서를_조회할_때마다_열람_로그가_쌓인다() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/forms/{form-id}", form.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                            .header(HttpHeaders.USER_AGENT, USER_AGENT)
                            .header("x-forwarded-for", CLIENT_IP + ", 10.0.0.1"))
                    .andExpect(status().isOk());
        }

        List<AdminFromViewLog> logs = viewLogs();
        assertEquals(2, logs.size());
        AdminFromViewLog log = logs.get(0);
        assertEquals(CLIENT_IP, log.getClientIp());
        assertEquals(USER_AGENT, log.getUserAgent());
        assertEquals(admin.getId(), log.getUser().getId());
        assertEquals(form.getId(), log.getForm().getId());
    }

    @Test
    void 지원자가_본인_원서를_조회하면_열람_로그를_남기지_않는다() throws Exception {
        mockMvc.perform(get("/forms/{form-id}", form.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
                .andExpect(status().isOk());

        assertEquals(0, adminFormViewLogRepository.count());
    }

    @Test
    void 관리자가_원서를_승인하면_변경_전후_상태가_남는다() throws Exception {
        mockMvc.perform(patch("/forms/{form-id}/approve", form.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .header(HttpHeaders.USER_AGENT, USER_AGENT)
                        .header("x-forwarded-for", CLIENT_IP))
                .andExpect(status().isNoContent());

        List<AdminFormStatusChangeLog> logs = statusChangeLogs();
        assertEquals(1, logs.size());
        AdminFormStatusChangeLog log = logs.get(0);
        assertEquals(FormStatus.FINAL_SUBMITTED, log.getBeforeStatus());
        assertEquals(FormStatus.APPROVED, log.getAfterStatus());
        assertEquals(CLIENT_IP, log.getClientIp());
        assertEquals(USER_AGENT, log.getUserAgent());
        assertEquals(admin.getId(), log.getUser().getId());
        assertEquals(form.getId(), log.getForm().getId());
        assertEquals(FormStatus.APPROVED, formRepository.findById(form.getId()).orElseThrow().getStatus());
    }

    @Test
    void 반려와_접수_처리도_각각_한_행씩_남는다() throws Exception {
        mockMvc.perform(patch("/forms/{form-id}/reject", form.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/forms/{form-id}/receive", form.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        List<AdminFormStatusChangeLog> logs = statusChangeLogs();
        assertEquals(2, logs.size());
        assertEquals(FormStatus.FINAL_SUBMITTED, logs.get(0).getBeforeStatus());
        assertEquals(FormStatus.REJECTED, logs.get(0).getAfterStatus());
        assertEquals(FormStatus.REJECTED, logs.get(1).getBeforeStatus());
        assertEquals(FormStatus.RECEIVED, logs.get(1).getAfterStatus());
    }

    @Test
    void 관리자가_아니면_상태를_바꿀_수_없고_로그도_남지_않는다() throws Exception {
        mockMvc.perform(patch("/forms/{form-id}/approve", form.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
                .andExpect(status().isUnauthorized());

        assertEquals(0, adminFormStatusChangeLogRepository.count());
        assertEquals(FormStatus.FINAL_SUBMITTED, formRepository.findById(form.getId()).orElseThrow().getStatus());
    }

    @Test
    void 존재하지_않는_원서의_상태를_바꾸면_로그가_남지_않는다() throws Exception {
        mockMvc.perform(patch("/forms/{form-id}/approve", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound());

        assertEquals(0, adminFormStatusChangeLogRepository.count());
    }

    @Test
    void 로그가_남은_원서를_삭제하면_로그도_함께_삭제된다() throws Exception {
        mockMvc.perform(get("/forms/{form-id}", form.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/forms/{form-id}/approve", form.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        formRepository.deleteById(form.getId());
        formRepository.flush();

        assertEquals(0, adminFormViewLogRepository.count());
        assertEquals(0, adminFormStatusChangeLogRepository.count());
    }

    // 연관 엔티티는 LAZY 프록시지만 id 조회는 초기화 없이 가능하다.
    private List<AdminFromViewLog> viewLogs() {
        return StreamSupport.stream(adminFormViewLogRepository.findAll().spliterator(), false).toList();
    }

    private List<AdminFormStatusChangeLog> statusChangeLogs() {
        return StreamSupport.stream(adminFormStatusChangeLogRepository.findAll().spliterator(), false)
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();
    }
}
