package com.bamdoliro.maru.infrastructure.neis;

import com.bamdoliro.maru.domain.form.domain.type.AllowedRegion;
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

    private static final Set<String> EXCEPTION_SCHOOLS = Set.of(
            "지평선중학교"
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
                .filter(this::isAccessibleSchool)
                .limit(10)
                .map(s -> SchoolResponse.builder()
                        .name(s.getSchoolName())
                        .location(s.getLocation())
                        .address((s.getAddress()))
                        .code(s.getStandardSchoolCode())
                        .build())
                .toList();
    }

    private boolean isAccessibleSchool(NeisSchoolResponse.SchoolInfo.Row school) {
        return AllowedRegion.isAllowed(school.getLocation()) ||
        EXCEPTION_SCHOOLS.contains(school.getSchoolName());
    }
}
