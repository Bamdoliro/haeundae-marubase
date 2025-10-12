package com.bamdoliro.maru.application.analysis;

import com.bamdoliro.maru.domain.form.domain.type.FormType;
import com.bamdoliro.maru.infrastructure.persistence.form.FormRepository;
import com.bamdoliro.maru.presentation.analysis.dto.request.GradeDistributionRequest;
import com.bamdoliro.maru.presentation.analysis.dto.response.GradeDistributionResponse;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@UseCase
public class QueryGradeDistributionUseCase {

    private final FormRepository formRepository;

    public List<GradeDistributionResponse> execute(GradeDistributionRequest request) {
        List<GradeDistributionResponse> result = new java.util.ArrayList<>(formRepository.findGradeGroupByTypeAndStatus(request.getStatusList())
                .stream()
                .map(GradeDistributionResponse::new)
                .toList());

        List<FormType> existingTypes = result
                .stream()
                .map(GradeDistributionResponse::getType)
                .toList();

        for (FormType formType : FormType.values()) {
            if (!existingTypes.contains(formType)) {
                result.add(new GradeDistributionResponse(formType, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
            }
        }

        // 메인 카테고리별 70% 커트라인 계산
        Map<FormType.Category, Double> firstRoundPercentiles = new HashMap<>();
        Map<FormType.Category, Double> totalPercentiles = new HashMap<>();

        List<FormType.Category> mainCategories = List.of(
            FormType.Category.REGULAR,
            FormType.Category.SPECIAL,
            FormType.Category.SUPERNUMERARY
        );

        for (FormType.Category category : mainCategories) {
            Double firstRoundPercentile = formRepository.findFirstRoundScoreAtPercentile(category, 0.7, request.getStatusList());
            Double totalPercentile = formRepository.findTotalScoreAtPercentile(category, 0.7, request.getStatusList());

            firstRoundPercentiles.put(category, firstRoundPercentile != null ? firstRoundPercentile : 0.0);
            totalPercentiles.put(category, totalPercentile != null ? totalPercentile : 0.0);
        }

        // 각 전형별로 해당 카테고리의 70% 커트라인 설정
        for (GradeDistributionResponse response : result) {
            FormType.Category category = response.getType().getCategory();
            response.setPercentiles(
                firstRoundPercentiles.get(category),
                totalPercentiles.get(category)
            );
        }

        return result;
    }
}