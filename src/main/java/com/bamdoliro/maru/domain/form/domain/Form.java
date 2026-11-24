package com.bamdoliro.maru.domain.form.domain;

import com.bamdoliro.maru.domain.auth.exception.AuthorityMismatchException;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.value.*;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.converter.LongEncryptedConverter;
import com.bamdoliro.maru.shared.config.TimeConfig;
import com.bamdoliro.maru.shared.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_form")
@Entity
public class Form extends BaseTimeEntity {

    @Column(name = "form_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Convert(converter = LongEncryptedConverter.class)
    @Column(nullable = true, unique = true)
    private Long examinationNumber;

    @Column(nullable = true)
    private String interviewNumber;

    @Embedded
    private Applicant applicant;

    @Embedded
    private Parent parent;

    @Embedded
    private Education education;

    @Embedded
    private Grade grade;

    @Embedded
    private Document document;

    @Embedded
    private Score score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FormType type;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private FormType originalType;

    @Column(nullable = false)
    private Boolean changedToRegular;

    @Column(nullable = false)
    private Boolean payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FormStatus status;

    // 지원자가 원서를 최종 제출한 시각. 관리자가 출력하는 서류의 서명란 날짜로 쓴다.
    // 이미 저장된 원서 행이 있는 DB에서 ddl-auto: update로 NOT NULL 컬럼을 바로 추가하면 실패하므로 nullable로 둔다.
    @Column(nullable = true)
    private LocalDateTime finalSubmittedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id", unique = true)
    private User user;

    @Builder
    public Form(Applicant applicant, Parent parent, Education education, Grade grade, Document document, FormType type, User user) {
        this.applicant = applicant;
        this.parent = parent;
        this.education = education;
        this.grade = grade;
        this.document = document;
        this.type = type;
        this.originalType = type;
        this.user = user;
        this.changedToRegular = false;
        this.payment = false;
        this.status = FormStatus.SUBMITTED;
    }

    public void submit() {
        this.status = FormStatus.FINAL_SUBMITTED;
        this.finalSubmittedAt = LocalDateTime.now(TimeConfig.SERVICE_ZONE_ID);
    }

    public void arrive() {
        this.status = FormStatus.ARRIVED;
    }

    public void notArrive() {
        this.status = FormStatus.NOT_ARRIVED;
    }

    public void updateScore(Score score) {
        this.score = score;
    }

    public void approve() {
        this.status = FormStatus.APPROVED;
    }

    public void reject() {
        this.status = FormStatus.REJECTED;
    }

    public void receive() {
        this.status = FormStatus.RECEIVED;
    }

    public void firstPass() {
        this.status = FormStatus.FIRST_PASSED;
    }

    public void firstFail() {
        this.status = FormStatus.FIRST_FAILED;
    }

    public void pass() {
        this.status = FormStatus.PASSED;
    }

    public void fail() {
        this.status = FormStatus.FAILED;
    }

    public void noShow() {
        this.status = FormStatus.NO_SHOW;
    }

    public void enter() { this.status = FormStatus.ENTERED; }

    public void payFee() {
        this.payment = true;
    }

    public void cancelPayment() {
        this.payment = false;
    }

    public void isApplicant(User user) {
        if (!this.user.equals(user)) {
            throw new AuthorityMismatchException();
        }
    }

    public void isApplicantOrAdmin(User user) {
        if (!user.isAdmin() && !this.user.equals(user)) {
            throw new AuthorityMismatchException();
        }
    }

    public boolean isRejected() {
        return status.equals(FormStatus.REJECTED);
    }

    public boolean isSubmitted() {
        return status.equals(FormStatus.SUBMITTED);
    }

    public boolean isFinalSubmitted() {
        return status.equals(FormStatus.FINAL_SUBMITTED);
    }

    public boolean isApproved() {
        return status.equals(FormStatus.APPROVED);
    }

    public boolean isReceived() {
        return isFirstPassed() != null || status.equals(FormStatus.RECEIVED);
    }

    public boolean isNoShow() {
        return status.equals(FormStatus.NO_SHOW);
    }

    public boolean isFirstPassedNow() {
        return status.equals(FormStatus.FIRST_PASSED);
    }

    public boolean isEntered() {
        return status.equals(FormStatus.ENTERED);
    }

    public boolean isPaid() {
        return payment;
    }

    public Boolean isFirstPassed() {
        if (isFirstPassedNow() || isPassed() != null || isNoShow()) {
            return true;
        }

        return isFirstFailedNow() ? false : null;
    }

    public boolean isFirstFailedNow() {
        return status.equals(FormStatus.FIRST_FAILED);
    }

    public boolean isPassedNow() {
        return status.equals(FormStatus.PASSED);
    }

    public Boolean isPassed() {
        if (isPassedNow() || isEntered()) {
            return true;
        }

        return isFailedNow() || isNoShow() ? false : null;
    }


    public boolean isFailedNow() {
        return status.equals(FormStatus.FAILED);
    }

    public void changeToRegularFirstRound(CalculateFormScoreService calculateFormScoreService) {
        this.changedToRegular = true;
        this.type = FormType.REGULAR;

        Double subjectGradeScore = calculateFormScoreService.calculateSubjectGradeScore(this);
        this.score.updateSubjectScore(subjectGradeScore);
    }

    public void update(Applicant applicant, Parent parent, Education education, Grade grade, Document document, FormType type) {
        this.applicant = applicant;
        this.parent = parent;
        this.education = education;
        this.grade = grade;
        this.document = document;
        this.type = type;
        this.originalType = type;
        this.payment = false;
        this.status = FormStatus.SUBMITTED;
    }

    public void assignExaminationNumber(Long examinationNumber) {
        this.examinationNumber = examinationNumber;
    }

    public void assignInterviewNumber(String interviewNumber) {
        this.interviewNumber = interviewNumber;
    }

    public boolean tookSecondRound() {
        return status.equals(FormStatus.PASSED) || status.equals(FormStatus.FAILED);
    }
}

