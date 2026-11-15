package com.bamdoliro.maru.domain.form.service;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.AchievementLevel;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.type.Gender;
import com.bamdoliro.maru.domain.form.domain.type.GraduationType;
import com.bamdoliro.maru.domain.form.domain.value.Account;
import com.bamdoliro.maru.domain.form.domain.value.Address;
import com.bamdoliro.maru.domain.form.domain.value.Applicant;
import com.bamdoliro.maru.domain.form.domain.value.Attendance;
import com.bamdoliro.maru.domain.form.domain.value.Document;
import com.bamdoliro.maru.domain.form.domain.value.Education;
import com.bamdoliro.maru.domain.form.domain.value.Grade;
import com.bamdoliro.maru.domain.form.domain.value.Parent;
import com.bamdoliro.maru.domain.form.domain.value.PhoneNumber;
import com.bamdoliro.maru.domain.form.domain.value.School;
import com.bamdoliro.maru.domain.form.domain.value.Subject;
import com.bamdoliro.maru.domain.form.domain.value.SubjectList;
import com.bamdoliro.maru.domain.form.domain.value.Teacher;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.shared.constants.Schedule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 원서 접수 테스트용 랜덤 원서를 만든다. 실제 지원자 데이터와 섞이지 않도록 유저는 isTestData = true로 만든다.
 * 주민등록번호와 전화번호의 중복 검사는 호출하는 쪽에서 한다.
 */
@Component
public class RandomFormGenerator {

    /**
     * 휴대전화 번호 가운데 자리가 0으로 시작하는 번호(010-0XXX-XXXX)는 실제로 개통되지 않으므로,
     * 테스트 유저가 실제 지원자의 회원가입 전화번호를 선점하는 일이 없다.
     */
    private static final String TEST_PHONE_NUMBER_PREFIX = "0100";

    private static final List<String> LAST_NAMES = List.of("김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신", "권");
    private static final List<String> FIRST_NAME_PARTS = List.of("민", "서", "지", "현", "우", "준", "하", "윤", "도", "은", "수", "예", "시", "재", "유", "건", "아", "연");
    private static final List<String> RELATIONS = List.of("부", "모");
    private static final List<String> BANKS = List.of("부산은행", "국민은행", "신한은행", "농협은행", "우리은행");
    private static final List<School> SCHOOLS = List.of(
            new School("부산중학교", "부산광역시", "부산광역시 동구 초량로40번길 29", "7150000"),
            new School("해운대중학교", "부산광역시", "부산광역시 해운대구 해운대로 123", "7150001"),
            new School("센텀중학교", "부산광역시", "부산광역시 해운대구 센텀동로 45", "7150002"),
            new School("동래중학교", "부산광역시", "부산광역시 동래구 명륜로 67", "7150003")
    );
    private static final List<String> SENTENCES = List.of(
            "스스로 계획을 세워 꾸준히 실천했습니다.",
            "친구들과 협력하여 과제를 끝까지 완성했습니다.",
            "실패한 경험을 돌아보며 다시 도전했습니다.",
            "관심 분야의 책을 읽고 정리하는 습관을 길렀습니다.",
            "본교에서 꿈을 구체화하고 싶습니다.",
            "다른 사람의 의견을 존중하며 함께 성장하고 싶습니다."
    );
    private static final List<String> SUBJECT_NAMES = List.of("국어", "사회", "수학", "과학", "영어");
    private static final List<AchievementLevel> ACHIEVEMENT_LEVELS = List.of(
            AchievementLevel.A, AchievementLevel.B, AchievementLevel.C, AchievementLevel.D, AchievementLevel.E
    );

    private final Random random;

    public RandomFormGenerator() {
        this(new Random());
    }

    public RandomFormGenerator(Random random) {
        this.random = random;
    }

    public TestApplicant generateApplicant() {
        Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
        // 원서 접수 시점(가을~겨울)의 중학교 3학년 = 올해 기준 만 15세가 되는 해에 태어난 학생
        int birthYear = Schedule.now().getYear() - 15;
        LocalDate birthday = LocalDate.of(birthYear, 1, 1).plusDays(random.nextInt(365));
        return new TestApplicant(randomName(), gender, birthday);
    }

    public String generatePhoneNumber() {
        return TEST_PHONE_NUMBER_PREFIX + digits(7);
    }

    /**
     * YYMMDD-GNNNNNN (14자). 2000년 이후 출생이므로 성별 자리는 남 3, 여 4.
     */
    public String generateRegistrationNumber(TestApplicant applicant) {
        String front = applicant.birthday().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String genderDigit = applicant.gender() == Gender.MALE ? "3" : "4";
        return front + "-" + genderDigit + digits(6);
    }

    public Form generate(User user, FormType type, TestApplicant applicant, String phoneNumber, String registrationNumber) {
        return Form.builder()
                .applicant(new Applicant(
                        applicant.name(),
                        new PhoneNumber(phoneNumber),
                        applicant.birthday(),
                        registrationNumber,
                        applicant.gender(),
                        "test" + digits(8) + "@example.com"
                ))
                .parent(new Parent(
                        randomName(),
                        new PhoneNumber(generatePhoneNumber()),
                        pick(RELATIONS),
                        new Address("48000", "부산광역시 해운대구 해운대로 " + (1 + random.nextInt(900)), (1 + random.nextInt(20)) + "층"),
                        new Account(digits(4) + "-" + digits(2) + "-" + digits(7), randomName(), pick(BANKS))
                ))
                .education(new Education(
                        GraduationType.EXPECTED,
                        (applicant.birthday().getYear() + 16) + "-02-10",
                        pick(SCHOOLS),
                        new Teacher(randomName(), new PhoneNumber("051" + digits(7)), new PhoneNumber(generatePhoneNumber()))
                ))
                .grade(new Grade(
                        new SubjectList(randomSubjects()),
                        randomAttendance(5),
                        randomAttendance(3),
                        randomAttendance(2)
                ))
                .document(new Document(randomParagraph(), randomParagraph(), randomParagraph()))
                .type(type)
                .user(user)
                .build();
    }

    private List<Subject> randomSubjects() {
        List<Subject> subjects = new ArrayList<>();
        for (int grade = 2; grade <= 3; grade++) {
            for (int semester = 1; semester <= 2; semester++) {
                for (String subjectName : SUBJECT_NAMES) {
                    subjects.add(new Subject(grade, semester, subjectName, pick(ACHIEVEMENT_LEVELS)));
                }
            }
        }
        return subjects;
    }

    private Attendance randomAttendance(int max) {
        return new Attendance(
                random.nextInt(max + 1),
                random.nextInt(max + 1),
                random.nextInt(max + 1),
                random.nextInt(max + 1)
        );
    }

    private String randomName() {
        return pick(LAST_NAMES) + pick(FIRST_NAME_PARTS) + pick(FIRST_NAME_PARTS);
    }

    private String randomParagraph() {
        return pick(SENTENCES) + " " + pick(SENTENCES) + " " + pick(SENTENCES);
    }

    private String digits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    public record TestApplicant(String name, Gender gender, LocalDate birthday) {
    }
}
