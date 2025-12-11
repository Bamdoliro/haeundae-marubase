package com.bamdoliro.maru.domain.form.domain;

import com.bamdoliro.maru.domain.auth.exception.AuthorityMismatchException;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.domain.form.domain.value.*;
import com.bamdoliro.maru.domain.form.service.CalculateFormScoreService;
import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.infrastructure.persistence.converter.LongEncryptedConverter;
import com.bamdoliro.maru.shared.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Convert(converter = LongEncryptedConverter.class)
    @Column(nullable = true, unique = true)
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id")
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

    public void changeToRegularSecondRound(CalculateFormScoreService calculateFormScoreService) {
        this.changedToRegular = true;
        this.type = FormType.REGULAR;
        Double subjectGradeScore = calculateFormScoreService.calculateSubjectGradeScore(this);
        this.score.updateSubjectScore(subjectGradeScore);

        Double selfDirectedScore = calculateFormScoreService.calculateSelfDirectedScoreToRegular(this);
        this.score.updateSecondRoundSocialScoreToRegular(selfDirectedScore);
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

