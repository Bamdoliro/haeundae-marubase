package com.bamdoliro.maru.application.fair;

import com.bamdoliro.maru.domain.fair.domain.Attendee;
import com.bamdoliro.maru.domain.fair.domain.Fair;
import com.bamdoliro.maru.infrastructure.persistence.fair.AttendeeRepository;
import com.bamdoliro.maru.presentation.fair.dto.response.FairDetailResponse;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@UseCase
public class QueryFairDetailUseCase {

    private final FairFacade fairFacade;
    private final AttendeeRepository attendeeRepository;

    public FairDetailResponse execute(Long fairId, String sort) {
        Fair fair = fairFacade.getFairDetail(fairId);

        List<Attendee> sortedAttendeeList = switch (sort) {
            case "name_asc" -> fair.getAttendeeList().stream()
                .sorted(Comparator.comparing(Attendee::getName))
                .collect(Collectors.toList());
            case "name_desc" -> fair.getAttendeeList().stream()
                .sorted(Comparator.comparing(Attendee::getName).reversed())
                .collect(Collectors.toList());
            default -> fair.getAttendeeList();
        };

        return new FairDetailResponse(fair, sortedAttendeeList, attendeeRepository);
    }
}
