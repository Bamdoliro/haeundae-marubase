package com.bamdoliro.maru.domain.form.service;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.type.Gender;
import com.bamdoliro.maru.domain.form.service.RandomFormGenerator.TestApplicant;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.user.domain.value.Password;
import com.bamdoliro.maru.shared.constants.Schedule;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomFormGeneratorTest {

    private final RandomFormGenerator generator = new RandomFormGenerator(new Random(42));

    @Test
    void 전화번호는_실제로_개통되지_않는_010_0으로_시작하는_11자리다() {
        for (int i = 0; i < 100; i++) {
            String phoneNumber = generator.generatePhoneNumber();
            assertTrue(phoneNumber.matches("0100\\d{7}"), phoneNumber);
        }
    }

    @Test
    void 주민등록번호는_생일과_성별에_맞는_14자리_형식이다() {
        for (int i = 0; i < 100; i++) {
            TestApplicant applicant = generator.generateApplicant();
            String registrationNumber = generator.generateRegistrationNumber(applicant);

            assertEquals(14, registrationNumber.length());
            assertTrue(registrationNumber.matches("\\d{6}-[34]\\d{6}"), registrationNumber);
            assertEquals(applicant.birthday().format(DateTimeFormatter.ofPattern("yyMMdd")), registrationNumber.substring(0, 6));
            assertEquals(applicant.gender() == Gender.MALE ? '3' : '4', registrationNumber.charAt(7));
            assertEquals(Schedule.now().getYear() - 15, applicant.birthday().getYear());
        }
    }

    @Test
    void 지정한_전형과_유저로_필드가_채워진_원서를_만든다() {
        User user = User.createTestData("01000000001", "김테스트", new Password("encoded"));
        TestApplicant applicant = generator.generateApplicant();

        Form form = generator.generate(user, FormType.ONE_PARENT, applicant, "01000000001", "110101-3000000");

        assertSame(user, form.getUser());
        assertEquals(FormType.ONE_PARENT, form.getType());
        assertEquals(applicant.name(), form.getApplicant().getName());
        assertEquals("01000000001", form.getApplicant().getPhoneNumber().getValue());
        assertEquals("110101-3000000", form.getApplicant().getRegistrationNumber());
        assertNotNull(form.getParent().getName());
        assertNotNull(form.getEducation().getSchool().getName());
        assertEquals(20, form.getGrade().getSubjectList().getValue().size());
        assertNotNull(form.getGrade().getAttendance1());
        assertFalse(form.getDocument().getLearningExperience().isBlank());
        assertFalse(form.getDocument().getStatementOfPurpose().isBlank());
        assertFalse(form.getDocument().getPersonality().isBlank());
        assertNull(form.getExaminationNumber());
        assertEquals(FormStatus.SUBMITTED, form.getStatus());
    }
}
