package com.bamdoliro.maru.infrastructure.neis;

import com.bamdoliro.maru.infrastructure.neis.feign.NeisClient;
import com.bamdoliro.maru.infrastructure.neis.feign.dto.response.NeisSchoolResponse;
import com.bamdoliro.maru.presentation.school.dto.response.SchoolResponse;
import com.bamdoliro.maru.shared.config.properties.NeisProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class SearchSchoolService {

    private final NeisProperties neisProperties;
    private final NeisClient neisClient;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_REGIONS = Set.of(
        "부산광역시",
        "광주광역시",
        "경상남도",
        "제주특별자치도",
        "충청북도",
        "세종특별자치시"
    );

    public List<SchoolResponse> execute(String q) throws JsonProcessingException {
        String htmlResponse1 = neisClient.getSchoolInfo(neisProperties.getKey(), q, "중학교");
        String htmlResponse2 = neisClient.getSchoolInfo(neisProperties.getKey(), q, "각종학교(중)");

        NeisSchoolResponse response1 = objectMapper.readValue(htmlResponse1, NeisSchoolResponse.class);
        NeisSchoolResponse response2 = objectMapper.readValue(htmlResponse2, NeisSchoolResponse.class);

        List<NeisSchoolResponse.SchoolInfo.Row> combinedSchoolInfo = new ArrayList<>();
        combinedSchoolInfo.addAll(response1.getSchoolInfo());
        combinedSchoolInfo.addAll(response2.getSchoolInfo());

        return combinedSchoolInfo.stream()
                .filter(s -> isAllowedRegion(s.getLocation()))
                .limit(10)
                .map(s -> SchoolResponse.builder()
                        .name(s.getSchoolName())
                        .location(s.getLocation())
                        .address((s.getAddress()))
                        .code(s.getStandardSchoolCode())
                        .build())
                .toList();
    }

    private boolean isAllowedRegion(String location) {
        return location != null && ALLOWED_REGIONS.contains(location);
    }
}
