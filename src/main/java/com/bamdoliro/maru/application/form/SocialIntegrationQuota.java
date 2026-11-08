package com.bamdoliro.maru.application.form;

/**
 * 사회통합전형 정원을 기회균등/사회다양성에 반씩 나누고,
 * 한쪽 지원자가 배정 인원보다 적으면 남는 자리를 다른 쪽에 넘긴다(넘겨받는 쪽 지원자 수를 넘지 않게).
 */
record SocialIntegrationQuota(int equalOpportunityCount, int societyDiversityCount) {

    static SocialIntegrationQuota of(int socialIntegrationCount, int equalOpportunityApplicants, int societyDiversityApplicants) {
        int equalOpportunityCount = (int) Math.round(socialIntegrationCount * 0.5);
        int societyDiversityCount = equalOpportunityCount;

        int equalOpportunityShortage = Math.max(0, equalOpportunityCount - equalOpportunityApplicants);
        int societyDiversityShortage = Math.max(0, societyDiversityCount - societyDiversityApplicants);

        int equalOpportunityRoom = Math.max(0, equalOpportunityApplicants - equalOpportunityCount);
        int societyDiversityRoom = Math.max(0, societyDiversityApplicants - societyDiversityCount);

        return new SocialIntegrationQuota(
                equalOpportunityCount + Math.min(societyDiversityShortage, equalOpportunityRoom),
                societyDiversityCount + Math.min(equalOpportunityShortage, societyDiversityRoom)
        );
    }
}
