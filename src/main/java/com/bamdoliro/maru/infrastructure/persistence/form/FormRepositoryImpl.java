package com.bamdoliro.maru.infrastructure.persistence.form;

import com.bamdoliro.maru.domain.form.domain.Form;
import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.infrastructure.persistence.form.vo.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.bamdoliro.maru.domain.form.domain.QForm.form;

@Repository
@RequiredArgsConstructor
public class FormRepositoryImpl implements FormRepositoryCustom {

    private static final String BUSAN = "부산광역시";

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Form> findByStatus(FormStatus status) {
        return queryFactory
                .selectFrom(form)
                .where(eqStatus(status))
                .orderBy(form.examinationNumber.asc())
                .fetch();
    }

    @Override
    public List<Form> findByType(FormType type) {
        return queryFactory
                .selectFrom(form)
                .where(eqType(type))
                .fetch();
    }

    @Override
    public List<Form> findByCategory(FormType.Category category) {
        List<FormType> matchingFormTypes = getFormTypesByCategory(category);

        return queryFactory
                .selectFrom(form)
                .where(form.type.in(matchingFormTypes))
                .fetch();
    }

    private BooleanExpression eqStatus(FormStatus status) {
        if (Objects.isNull(status)) {
            return null;
        }

        return form.status.eq(status);
    }

    private BooleanExpression eqType(FormType type) {
        if (Objects.isNull(type)) {
            return null;
        }

        return form.type.eq(type);
    }

    private List<FormType> getFormTypesByCategory(FormType.Category category) {
        return Stream.of(FormType.values())
                .filter(formType -> formType.categoryEquals(category))
                .toList();
    }

    @Override
    public List<Form> findApprovedSpecialForm() {
        return queryFactory
                .selectFrom(form)
                .where(
                        form.status.eq(FormStatus.APPROVED)
                                .and(
                                        form.type.eq(FormType.REGULAR).not()
                                                .and(form.type.eq(FormType.NATIONAL_VETERANS_EDUCATION).not())
                                                .and(form.type.eq(FormType.SPECIAL_ADMISSION).not())
                                )
                )
                .orderBy(form.score.firstRoundScore.desc())
                .fetch();
    }

    @Override
    public List<Form> findApprovedRegularForm() {
        return queryFactory
                .selectFrom(form)
                .where(
                        form.status.eq(FormStatus.APPROVED)
                                .and(
                                        form.type.eq(FormType.REGULAR)
                                                .or(form.changedToRegular.isTrue())
                                )
                )
                .orderBy(form.score.firstRoundScore.desc())
                .fetch();
    }

    @Override
    public List<Form> findApprovedSupernumeraryForm() {
        return queryFactory
                .selectFrom(form)
                .where(
                        form.status.eq(FormStatus.APPROVED)
                                .and(
                                        form.type.eq(FormType.SPECIAL_ADMISSION)
                                                .or(form.type.eq(FormType.NATIONAL_VETERANS_EDUCATION))
                                )
                )
                .orderBy(form.score.firstRoundScore.desc())
                .fetch();
    }

    @Override
    public List<Form> findFirstPassedSpecialForm() {
        return queryFactory
                .selectFrom(form)
                .where(
                        form.status.eq(FormStatus.FIRST_PASSED)
                                .and(
                                        form.type.eq(FormType.REGULAR).not()
                                                .and(form.type.eq(FormType.NATIONAL_VETERANS_EDUCATION).not())
                                                .and(form.type.eq(FormType.SPECIAL_ADMISSION).not())
                                )
                )
                .orderBy(
                        form.score.selfDirectedScore.add(form.score.personalityScore).desc(),
                        form.score.subjectGradeScore.desc(),
                        form.score.thirdGradeSecondSemesterSubjectGradeScore.desc().nullsLast(),
                        form.score.thirdGradeFirstSemesterSubjectGradeScore.desc().nullsLast(),
                        form.score.secondGradeSecondSemesterSubjectGradeScore.desc().nullsLast(),
                        form.score.secondGradeFirstSemesterSubjectGradeScore.desc().nullsLast(),
                        form.applicant.birthday.asc()
                )
                .fetch();
    }

    @Override
    public List<Form> findFirstPassedRegularForm() {
        return queryFactory
                .selectFrom(form)
                .where(
                        form.status.eq(FormStatus.FIRST_PASSED)
                                .and(
                                        form.type.eq(FormType.REGULAR)
                                                .or(form.changedToRegular.isTrue())
                                )
                )
                .orderBy(
                        form.score.selfDirectedScore.add(form.score.personalityScore).desc(),
                        form.score.subjectGradeScore.desc(),
                        form.score.thirdGradeSecondSemesterSubjectGradeScore.desc().nullsLast(),
                        form.score.thirdGradeFirstSemesterSubjectGradeScore.desc().nullsLast(),
                        form.score.secondGradeSecondSemesterSubjectGradeScore.desc().nullsLast(),
                        form.score.secondGradeFirstSemesterSubjectGradeScore.desc().nullsLast(),
                        form.applicant.birthday.asc()
                )
                .fetch();
    }

    @Override
    public List<Form> findFirstPassedSupernumeraryForm() {
        return queryFactory
                .selectFrom(form)
                .where(
                        form.status.eq(FormStatus.FIRST_PASSED)
                                .and(
                                        form.type.eq(FormType.SPECIAL_ADMISSION)
                                                .or(form.type.eq(FormType.NATIONAL_VETERANS_EDUCATION))
                                )
                )
                .orderBy(
                        form.score.selfDirectedScore.add(form.score.personalityScore).desc(),
                        form.score.subjectGradeScore.desc(),
                        form.score.thirdGradeSecondSemesterSubjectGradeScore.desc().nullsLast(),
                        form.score.thirdGradeFirstSemesterSubjectGradeScore.desc().nullsLast(),
                        form.score.secondGradeSecondSemesterSubjectGradeScore.desc().nullsLast(),
                        form.score.secondGradeFirstSemesterSubjectGradeScore.desc().nullsLast(),
                        form.applicant.birthday.asc()
                )
                .fetch();
    }

    @Override
    public List<Form> findFirstRoundForm() {
        return queryFactory
                .selectFrom(form)
                .where(form.status.eq(FormStatus.FIRST_PASSED)
                        .or(form.status.eq(FormStatus.FIRST_FAILED))
                )
                .fetch();
    }

    @Override
    public List<Form> findMeisterTalentFirstRoundForm() {
        return queryFactory
                .selectFrom(form)
                .where(form.status.eq(FormStatus.FIRST_PASSED))
                .fetch();
    }

    @Override
    public List<Form> findNotExistsMeisterTalentAndChangedToRegularFirstRoundForm() {
        return queryFactory
                .selectFrom(form)
                .where(form.status.eq(FormStatus.FIRST_PASSED)
                                .and(form.changedToRegular.isFalse())
                 )
                .fetch();
    }

    @Override
    public List<Form> findChangedToRegularFirstRoundForm() {
        return queryFactory
                .selectFrom(form)
                .where(form.status.eq(FormStatus.FIRST_PASSED)
                        .and(form.type.eq(FormType.REGULAR)
                                .and(form.changedToRegular.isTrue()))
                )
                .fetch();
    }

    @Override
    public List<Form> findSecondRoundForm() {
        return queryFactory
                .selectFrom(form)
                .where(form.status.eq(FormStatus.FAILED)
                        .or(form.status.eq(FormStatus.PASSED))
                )
                .fetch();
    }

    @Override
    public List<Form> findByFormIdList(List<Long> idList) {
        return queryFactory
                .selectFrom(form)
                .where(form.id.in(idList))
                .orderBy(form.id.asc())
                .fetch();
    }

    /**
     * 조회한 원서 행에 비관적 쓰기 락(SELECT ... FOR UPDATE)을 건다. 트랜잭션 안에서만 호출해야 한다.
     * id 오름차순으로 잠가서 여러 원서를 함께 처리하는 요청끼리 교착 상태가 생기지 않게 한다.
     */
    @Override
    public List<Form> findByFormIdListForUpdate(List<Long> idList) {
        return queryFactory
                .selectFrom(form)
                .where(form.id.in(idList))
                .orderBy(form.id.asc())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

    @Override
    public List<FormUrlVo> findFormUrlByFormIdList(List<Long> idList) {
        return queryFactory
                .select(new QFormUrlVo(
                        form.examinationNumber,
                        form.applicant.name,
                        form.user.uuid
                ))
                .from(form)
                .where(form.id.in(idList))
                .fetch();
    }

    @Override
    public List<NumberOfApplicantsVo> findTypeAndCountGroupByType() {
        return queryFactory
                .select(new QNumberOfApplicantsVo(
                        form.type,
                        form.count()
                ))
                .from(form)
                .groupBy(form.type)
                .fetch();
    }

    @Override
    public List<NumberOfApplicantsVo> findOriginalTypeAndCountGroupByType() {
        return queryFactory
                .select(new QNumberOfApplicantsVo(
                        form.originalType,
                        form.count()
                ))
                .from(form)
                .groupBy(form.originalType)
                .fetch();
    }

    @Override
    public List<GradeVo> findGradeGroupByTypeAndStatus(List<FormStatus> round) {
        return queryFactory
                .select(new QGradeVo(
                        form.type,
                        form.score.firstRoundScore.max(),
                        form.score.firstRoundScore.min(),
                        form.score.firstRoundScore.avg(),
                        form.score.totalScore.max(),
                        form.score.totalScore.min(),
                        form.score.totalScore.avg()
                ))
                .from(form)
                .where(form.status.in(round))
                .groupBy(form.type)
                .fetch();
    }

    @Override
    public List<SchoolStatusVo> findSchoolByAddress(List<FormStatus> round, String keyword) {
        return queryFactory
                .select(new QSchoolStatusVo(
                        form.applicant.name,
                        form.education.school.name,
                        form.education.school.address
                ))
                .from(form)
                .where(
                        form.status.in(round),
                        isInRegion(keyword.trim())
                )
                .fetch();
    }

    @Override
    public List<SchoolStatusVo> findNotBusanSchool(List<FormStatus> round) {
        return queryFactory
                .select(new QSchoolStatusVo(
                        form.applicant.name,
                        form.education.school.name,
                        form.education.school.address
                ))
                .from(form)
                .where(
                        form.status.in(round),
                        hasSchoolRegion(),
                        isInRegion(BUSAN).not()
                )
                .fetch();
    }

    /**
     * 학교 주소 또는 소재지 중 하나라도 region과 정확히 같거나 "region + 공백"으로 시작하면 해당 지역으로 본다.
     * findSchoolByAddress(부산)와 findNotBusanSchool은 이 판정을 공유하므로 서로 겹치지 않는다.
     */
    private BooleanExpression isInRegion(String region) {
        return matchesRegion(form.education.school.address, region)
                .or(matchesRegion(form.education.school.location, region));
    }

    private BooleanExpression matchesRegion(StringPath field, String region) {
        StringExpression trimmed = field.trim();
        return field.isNotNull()
                .and(trimmed.eq(region).or(trimmed.startsWith(region + " ")));
    }

    private BooleanExpression hasSchoolRegion() {
        return hasText(form.education.school.address)
                .or(hasText(form.education.school.location));
    }

    private BooleanExpression hasText(StringPath field) {
        return field.isNotNull().and(field.trim().isNotEmpty());
    }

    @Override
    public List<Long> findAllExaminationNumber() {
        return queryFactory
                .select(form.examinationNumber)
                .from(form)
                .fetch();
    }

    @Override
    public Double findFirstRoundScoreAtPercentile(FormType.Category category, double percentile, List<FormStatus> statusList) {
        List<FormType> matchingFormTypes = getFormTypesByCategory(category);

        // 전체 개수 조회
        long totalCount = queryFactory
                .selectFrom(form)
                .where(form.type.in(matchingFormTypes)
                        .and(form.status.in(statusList))
                        .and(form.score.firstRoundScore.isNotNull()))
                .fetchCount();

        if (totalCount == 0) {
            return 0.0;
        }

        // 70% 지점 계산
        long targetPosition = Math.round(totalCount * percentile);
        if (targetPosition == 0) {
            targetPosition = 1;
        }

        // OFFSET + LIMIT으로 해당 순위의 점수만 조회
        Double score = queryFactory
                .select(form.score.firstRoundScore)
                .from(form)
                .where(form.type.in(matchingFormTypes)
                        .and(form.status.in(statusList))
                        .and(form.score.firstRoundScore.isNotNull()))
                .orderBy(form.score.firstRoundScore.desc())
                .offset(targetPosition - 1)
                .limit(1)
                .fetchOne();

        return score != null ? score : 0.0;
    }

    @Override
    public Double findTotalScoreAtPercentile(FormType.Category category, double percentile, List<FormStatus> statusList) {
        List<FormType> matchingFormTypes = getFormTypesByCategory(category);

        // 전체 개수 조회
        long totalCount = queryFactory
                .selectFrom(form)
                .where(form.type.in(matchingFormTypes)
                        .and(form.status.in(statusList))
                        .and(form.score.totalScore.isNotNull()))
                .fetchCount();

        if (totalCount == 0) {
            return 0.0;
        }

        // 70% 지점 계산
        long targetPosition = Math.round(totalCount * percentile);
        if (targetPosition == 0) {
            targetPosition = 1;
        }

        // OFFSET + LIMIT으로 해당 순위의 점수만 조회
        Double score = queryFactory
                .select(form.score.totalScore)
                .from(form)
                .where(form.type.in(matchingFormTypes)
                        .and(form.status.in(statusList))
                        .and(form.score.totalScore.isNotNull()))
                .orderBy(form.score.totalScore.desc())
                .offset(targetPosition - 1)
                .limit(1)
                .fetchOne();

        return score != null ? score : 0.0;
    }

    @Override
    public List<SchoolStatusVo> findAllForms(List<FormStatus> round) {
        return queryFactory
                .select(new QSchoolStatusVo(
                        form.applicant.name,
                        form.education.school.name,
                        form.education.school.address
                ))
                .from(form)
                .where(form.status.in(round))
                .fetch();
    }
}