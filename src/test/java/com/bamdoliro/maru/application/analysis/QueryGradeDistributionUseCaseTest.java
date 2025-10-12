package com.bamdoliro.maru.application.analysis;

import com.bamdoliro.maru.domain.form.domain.type.FormStatus;
import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.infrastructure.persistence.form.vo.GradeVo;
import com.bamdoliro.maru.presentation.analysis.dto.request.GradeDistributionRequest;
import com.bamdoliro.maru.presentation.analysis.dto.response.GradeDistributionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueryGradeDistributionUseCaseTest {

    @InjectMocks
    private QueryGradeDistributionUseCase queryGradeDistributionUseCase;

    @Mock
    private FormRepository formRepository;

    @Test
    void 어드민이_1차_합격자들의_성적_분포를_조회한다() {
        // given
        List<GradeVo> voList = List.of(
                new GradeVo(FormType.REGULAR, 238.241, 160.278, 208.963, null, null, null),
                new GradeVo(FormType.NATIONAL_VETERANS, 158.758, 103.004, 147.213, null, null, null),
                new GradeVo(FormType.NATIONAL_BASIC_LIVING, 158.758, 103.004, 147.213, null, null, null),
                new GradeVo(FormType.FROM_NORTH_KOREA, 158.758, 103.004, 147.213, null, null, null),
                new GradeVo(FormType.SPECIAL_ADMISSION, 158.758, 103.004, 147.213, null, null, null)
        );
        List<FormStatus> round = List.of(FormStatus.FIRST_PASSED, FormStatus.FAILED, FormStatus.PASSED);
        given(formRepository.findGradeGroupByTypeAndStatus(round)).willReturn(voList);
        given(formRepository.findFirstRoundScoreAtPercentile(any(FormType.Category.class), eq(0.7), eq(round)))
            .willReturn(0.0);
        given(formRepository.findTotalScoreAtPercentile(any(FormType.Category.class), eq(0.7), eq(round)))
            .willReturn(0.0);
        GradeDistributionRequest request = new GradeDistributionRequest(round);

        // when
        List<GradeDistributionResponse> responseList = queryGradeDistributionUseCase.execute(request);

        // then
        assertEquals(responseList.size(), FormType.values().length);
        assertEquals(responseList.stream()
                .filter(response -> response.getFirstRoundAvg() != 0)
                .toList()
                .size(), voList.size());
        verify(formRepository, times(1)).findGradeGroupByTypeAndStatus(round);
    }

    @Test
    void 어드민이_2차_전형자들의_성적_분포를_조회한다() {
        // given
        List<GradeVo> voList = List.of(
                new GradeVo(FormType.REGULAR, 238.241, 160.278, 208.963, 208.758, 153.004, 197.213),
                new GradeVo(FormType.NATIONAL_VETERANS, 158.758, 103.004, 147.213, 208.758, 153.004, 197.213),
                new GradeVo(FormType.NATIONAL_BASIC_LIVING, 158.758, 103.004, 147.213, 208.758, 153.004, 197.213),
                new GradeVo(FormType.FROM_NORTH_KOREA, 158.758, 103.004, 147.213, 208.758, 153.004, 197.213),
                new GradeVo(FormType.SPECIAL_ADMISSION, 149.559, 149.559, 149.559, 199.559, 199.559, 199.559)
        );
        List<FormStatus> round = List.of(FormStatus.FAILED, FormStatus.PASSED);
        given(formRepository.findGradeGroupByTypeAndStatus(round)).willReturn(voList);
        given(formRepository.findFirstRoundScoreAtPercentile(any(FormType.Category.class), eq(0.7), eq(round)))
            .willReturn(0.0);
        given(formRepository.findTotalScoreAtPercentile(any(FormType.Category.class), eq(0.7), eq(round)))
            .willReturn(0.0);
        GradeDistributionRequest request = new GradeDistributionRequest(round);

        // when
        List<GradeDistributionResponse> responseList = queryGradeDistributionUseCase.execute(request);

        // then
        assertEquals(responseList.size(), FormType.values().length);
        assertEquals(responseList.stream()
                .filter(response -> response.getFirstRoundAvg() != 0)
                .toList()
                .size(), voList.size());
        verify(formRepository, times(1)).findGradeGroupByTypeAndStatus(round);
    }

    @Test
    void 어드민이_최종_합격자들의_성적_분포를_조회한다() {
        // given
        List<GradeVo> voList = List.of(
                new GradeVo(FormType.REGULAR, 238.241, 160.278, 208.963, 208.758, 153.004, 197.213),
                new GradeVo(FormType.NATIONAL_VETERANS, 158.758, 103.004, 147.213, 208.758, 153.004, 197.213),
                new GradeVo(FormType.NATIONAL_BASIC_LIVING, 158.758, 103.004, 147.213, 208.758, 153.004, 197.213),
                new GradeVo(FormType.FROM_NORTH_KOREA, 158.758, 103.004, 147.213, 208.758, 153.004, 197.213),
                new GradeVo(FormType.SPECIAL_ADMISSION, 149.559, 149.559, 149.559, 199.559, 199.559, 199.559)
        );
        List<FormStatus> round = List.of(FormStatus.PASSED);
        given(formRepository.findGradeGroupByTypeAndStatus(round)).willReturn(voList);
        given(formRepository.findFirstRoundScoreAtPercentile(any(FormType.Category.class), eq(0.7), eq(round)))
            .willReturn(0.0);
        given(formRepository.findTotalScoreAtPercentile(any(FormType.Category.class), eq(0.7), eq(round)))
            .willReturn(0.0);
        GradeDistributionRequest request = new GradeDistributionRequest(round);

        // when
        List<GradeDistributionResponse> responseList = queryGradeDistributionUseCase.execute(request);

        // then
        assertEquals(responseList.size(), FormType.values().length);
        assertEquals(responseList.stream()
                .filter(response -> response.getFirstRoundAvg() != 0)
                .toList()
                .size(), voList.size());
        verify(formRepository, times(1)).findGradeGroupByTypeAndStatus(round);
    }

    @Test
    void 성적분포와_70퍼센트_커트라인을_조회한다() {
        // given
        List<GradeVo> voList = List.of(
            new GradeVo(FormType.REGULAR, 350.0, 200.0, 275.0, 600.0, 400.0, 500.0),
            new GradeVo(FormType.NATIONAL_VETERANS, 340.0, 180.0, 260.0, 580.0, 380.0, 480.0)
        );
        List<FormStatus> round = List.of(FormStatus.RECEIVED, FormStatus.FIRST_PASSED);
        GradeDistributionRequest request = new GradeDistributionRequest(round);

        given(formRepository.findGradeGroupByTypeAndStatus(round)).willReturn(voList);
        given(formRepository.findFirstRoundScoreAtPercentile(eq(FormType.Category.REGULAR), eq(0.7), eq(round)))
            .willReturn(280.0);
        given(formRepository.findTotalScoreAtPercentile(eq(FormType.Category.REGULAR), eq(0.7), eq(round)))
            .willReturn(520.0);
        given(formRepository.findFirstRoundScoreAtPercentile(eq(FormType.Category.SPECIAL), eq(0.7), eq(round)))
            .willReturn(270.0);
        given(formRepository.findTotalScoreAtPercentile(eq(FormType.Category.SPECIAL), eq(0.7), eq(round)))
            .willReturn(510.0);
        given(formRepository.findFirstRoundScoreAtPercentile(eq(FormType.Category.SUPERNUMERARY), eq(0.7), eq(round)))
            .willReturn(0.0);
        given(formRepository.findTotalScoreAtPercentile(eq(FormType.Category.SUPERNUMERARY), eq(0.7), eq(round)))
            .willReturn(0.0);

        // when
        List<GradeDistributionResponse> result = queryGradeDistributionUseCase.execute(request);

        // then
        assertThat(result).hasSize(FormType.values().length);

        GradeDistributionResponse regularResponse = result.stream()
            .filter(r -> r.getType() == FormType.REGULAR)
            .findFirst()
            .orElseThrow();

        assertThat(regularResponse.getFirstRoundMax()).isEqualTo(350.0);
        assertThat(regularResponse.getFirstRoundSeventyPercentile()).isEqualTo(280.0);
        assertThat(regularResponse.getTotalSeventyPercentile()).isEqualTo(520.0);

        GradeDistributionResponse meisterTalentResponse = result.stream()
            .filter(r -> r.getType() == FormType.NATIONAL_VETERANS)
            .findFirst()
            .orElseThrow();

        assertThat(meisterTalentResponse.getFirstRoundSeventyPercentile()).isEqualTo(270.0);
        assertThat(meisterTalentResponse.getTotalSeventyPercentile()).isEqualTo(510.0);
    }

    @Test
    void 데이터가_없는_전형은_기본값으로_채워진다() {
        // given
        List<GradeVo> voList = List.of(
            new GradeVo(FormType.REGULAR, 350.0, 200.0, 275.0, 600.0, 400.0, 500.0)
        );
        List<FormStatus> round = List.of(FormStatus.RECEIVED, FormStatus.FIRST_PASSED);
        GradeDistributionRequest request = new GradeDistributionRequest(round);

        given(formRepository.findGradeGroupByTypeAndStatus(round)).willReturn(voList);
        given(formRepository.findFirstRoundScoreAtPercentile(any(FormType.Category.class), eq(0.7), eq(round)))
            .willReturn(null);
        given(formRepository.findTotalScoreAtPercentile(any(FormType.Category.class), eq(0.7), eq(round)))
            .willReturn(null);

        // when
        List<GradeDistributionResponse> result = queryGradeDistributionUseCase.execute(request);

        // then
        assertThat(result).hasSize(FormType.values().length);

        GradeDistributionResponse missingTypeResponse = result.stream()
            .filter(r -> r.getType() == FormType.NATIONAL_VETERANS)
            .findFirst()
            .orElseThrow();

        assertThat(missingTypeResponse.getFirstRoundMax()).isEqualTo(0.0);
        assertThat(missingTypeResponse.getFirstRoundSeventyPercentile()).isEqualTo(0.0);
        assertThat(missingTypeResponse.getTotalSeventyPercentile()).isEqualTo(0.0);
    }

    @Test
    void 같은_카테고리의_전형들은_동일한_70퍼센트_커트라인을_가진다() {
        // given
        List<GradeVo> voList = List.of(
            new GradeVo(FormType.REGULAR, 350.0, 200.0, 275.0, 600.0, 400.0, 500.0),
            new GradeVo(FormType.NATIONAL_VETERANS, 340.0, 180.0, 260.0, 580.0, 380.0, 480.0),
            new GradeVo(FormType.NATIONAL_BASIC_LIVING, 330.0, 170.0, 250.0, 570.0, 370.0, 470.0)
        );
        List<FormStatus> round = List.of(FormStatus.RECEIVED, FormStatus.FIRST_PASSED);
        GradeDistributionRequest request = new GradeDistributionRequest(round);

        given(formRepository.findGradeGroupByTypeAndStatus(round)).willReturn(voList);
        given(formRepository.findFirstRoundScoreAtPercentile(eq(FormType.Category.REGULAR), eq(0.7), eq(round)))
            .willReturn(280.0);
        given(formRepository.findTotalScoreAtPercentile(eq(FormType.Category.REGULAR), eq(0.7), eq(round)))
            .willReturn(520.0);
        given(formRepository.findFirstRoundScoreAtPercentile(eq(FormType.Category.SPECIAL), eq(0.7), eq(round)))
            .willReturn(270.0);
        given(formRepository.findTotalScoreAtPercentile(eq(FormType.Category.SPECIAL), eq(0.7), eq(round)))
            .willReturn(510.0);
        given(formRepository.findFirstRoundScoreAtPercentile(eq(FormType.Category.SUPERNUMERARY), eq(0.7), eq(round)))
            .willReturn(0.0);
        given(formRepository.findTotalScoreAtPercentile(eq(FormType.Category.SUPERNUMERARY), eq(0.7), eq(round)))
            .willReturn(0.0);

        // when
        List<GradeDistributionResponse> result = queryGradeDistributionUseCase.execute(request);

        // then
        GradeDistributionResponse regularResponse = result.stream()
            .filter(r -> r.getType() == FormType.REGULAR)
            .findFirst()
            .orElseThrow();

        GradeDistributionResponse meisterTalentResponse = result.stream()
            .filter(r -> r.getType() == FormType.NATIONAL_VETERANS)
            .findFirst()
            .orElseThrow();

        GradeDistributionResponse socialIntegrationResponse = result.stream()
            .filter(r -> r.getType() == FormType.NATIONAL_BASIC_LIVING)
            .findFirst()
            .orElseThrow();

        // REGULAR 카테고리는 REGULAR만 포함
        assertThat(regularResponse.getFirstRoundSeventyPercentile()).isEqualTo(280.0);
        assertThat(regularResponse.getTotalSeventyPercentile()).isEqualTo(520.0);

        // SPECIAL 카테고리는 MEISTER_TALENT, SOCIAL_INTEGRATION 등을 포함
        assertThat(meisterTalentResponse.getFirstRoundSeventyPercentile()).isEqualTo(270.0);
        assertThat(meisterTalentResponse.getTotalSeventyPercentile()).isEqualTo(510.0);
        assertThat(socialIntegrationResponse.getFirstRoundSeventyPercentile()).isEqualTo(270.0);
        assertThat(socialIntegrationResponse.getTotalSeventyPercentile()).isEqualTo(510.0);
    }
}