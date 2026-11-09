package com.bamdoliro.maru.application.form;

/**
 * 사회통합전형 정원을 기회균등/사회다양성에 반씩 나눈다.
 * 기회균등을 먼저 지원자 수 기준으로 확정해 남는 몫을 사회다양성에 넘기고,
 * 이어서 사회다양성을 확정하면서 남는 몫을 기회균등의 남은 여유분 한도 안에서 되돌려준다.
 * 두 배정 인원의 합은 항상 사회통합 정원 이하이다.
 */
record SocialIntegrationQuota(int equalOpportunityCount, int societyDiversityCount) {

    static SocialIntegrationQuota of(int socialIntegrationCount, int equalOpportunityApplicants, int societyDiversityApplicants) {
        int equalOpportunityCount = (int) Math.round(socialIntegrationCount * 0.5);
        int societyDiversityCount = socialIntegrationCount - equalOpportunityCount;

        if (equalOpportunityApplicants < equalOpportunityCount) {
            societyDiversityCount += equalOpportunityCount - equalOpportunityApplicants;
            equalOpportunityCount = equalOpportunityApplicants;
        }

        if (societyDiversityApplicants < societyDiversityCount) {
            int gap = societyDiversityCount - societyDiversityApplicants;
            equalOpportunityCount += Math.min(gap, equalOpportunityApplicants - equalOpportunityCount);
            societyDiversityCount = societyDiversityApplicants;
        }

        return new SocialIntegrationQuota(equalOpportunityCount, societyDiversityCount);
    }
}
