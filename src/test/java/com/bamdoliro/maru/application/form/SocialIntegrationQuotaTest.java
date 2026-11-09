package com.bamdoliro.maru.application.form;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

        assertEquals(10, quota.equalOpportunityCount());
        assertEquals(44, quota.societyDiversityCount());
    }

    @Test
    void 사회다양성이_미달이면_남는_자리를_기회균등에_넘긴다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(54, 50, 10);

        assertEquals(44, quota.equalOpportunityCount());
        assertEquals(10, quota.societyDiversityCount());
    }

    @Test
    void 사회다양성_미달분을_기회균등에_넘겨도_합은_정원과_같다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(54, 60, 10);

        assertEquals(44, quota.equalOpportunityCount());
        assertEquals(10, quota.societyDiversityCount());
    }

    @Test
    void 넘겨받는_인원은_받는_쪽_지원자_수를_넘지_않는다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(54, 10, 30);

        assertEquals(10, quota.equalOpportunityCount());
        assertEquals(30, quota.societyDiversityCount());
    }

    @Test
    void 양쪽_모두_미달이면_각자_지원자_수만큼_배정한다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(54, 10, 10);

        assertEquals(10, quota.equalOpportunityCount());
        assertEquals(10, quota.societyDiversityCount());
    }

    @Test
    void 사회통합_정원이_홀수면_기회균등에_한_명_더_배정하고_합은_정원과_같다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(53, 40, 40);

        assertEquals(27, quota.equalOpportunityCount());
        assertEquals(26, quota.societyDiversityCount());
    }

    @Test
    void 사회통합_정원이_0이면_양쪽_모두_0명이다() {
        SocialIntegrationQuota quota = SocialIntegrationQuota.of(0, 3, 2);

        assertEquals(0, quota.equalOpportunityCount());
        assertEquals(0, quota.societyDiversityCount());
    }

    @Test
    void 배정_인원의_합은_사회통합_정원을_넘지_않는다() {
        for (int total = 0; total <= 60; total++) {
            for (int equalOpportunity = 0; equalOpportunity <= 70; equalOpportunity++) {
                for (int societyDiversity = 0; societyDiversity <= 70; societyDiversity++) {
                    SocialIntegrationQuota quota = SocialIntegrationQuota.of(total, equalOpportunity, societyDiversity);
                    String input = "of(%d, %d, %d)".formatted(total, equalOpportunity, societyDiversity);

                    assertThat(quota.equalOpportunityCount() + quota.societyDiversityCount())
                            .as(input).isLessThanOrEqualTo(total);
                    // 지원자가 정원 이상이면 정원을 모두 채운다.
                    assertThat(quota.equalOpportunityCount() + quota.societyDiversityCount())
                            .as(input).isEqualTo(Math.min(total, equalOpportunity + societyDiversity));
                    assertThat(quota.equalOpportunityCount()).as(input).isBetween(0, equalOpportunity);
                    assertThat(quota.societyDiversityCount()).as(input).isBetween(0, societyDiversity);
                }
            }
        }
    }
}
