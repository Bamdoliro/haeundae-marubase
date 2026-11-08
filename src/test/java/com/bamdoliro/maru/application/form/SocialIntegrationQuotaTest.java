package com.bamdoliro.maru.application.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SocialIntegrationQuotaTest {

    @Test
    void 양쪽_모두_정원_이상이면_반반으로_나눈다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(54, 40, 40);

        assertEquals(27, quota.equalOpportunityCount());
        assertEquals(27, quota.societyDiversityCount());
    }

    @Test
    void 기회균등이_미달이면_남는_자리를_사회다양성에_넘긴다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(54, 10, 50);

        assertEquals(27, quota.equalOpportunityCount());
        assertEquals(44, quota.societyDiversityCount());
    }

    @Test
    void 사회다양성이_미달이면_남는_자리를_기회균등에_넘긴다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(54, 50, 10);

        assertEquals(44, quota.equalOpportunityCount());
        assertEquals(27, quota.societyDiversityCount());
    }

    @Test
    void 넘겨받는_인원은_받는_쪽_지원자_수를_넘지_않는다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(54, 10, 30);

        assertEquals(27, quota.equalOpportunityCount());
        assertEquals(30, quota.societyDiversityCount());
    }

    @Test
    void 양쪽_모두_미달이면_넘길_자리가_없다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(54, 10, 10);

        assertEquals(27, quota.equalOpportunityCount());
        assertEquals(27, quota.societyDiversityCount());
    }

    @Test
    void 사회통합_정원이_0이면_양쪽_모두_0명이다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(0, 3, 2);

        assertEquals(0, quota.equalOpportunityCount());
        assertEquals(0, quota.societyDiversityCount());
    }
}
