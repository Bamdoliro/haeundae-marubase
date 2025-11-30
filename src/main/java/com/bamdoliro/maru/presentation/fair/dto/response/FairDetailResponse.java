package com.bamdoliro.maru.presentation.fair.dto.response;

import com.bamdoliro.maru.domain.fair.domain.Attendee;
import com.bamdoliro.maru.domain.fair.domain.Fair;
import com.bamdoliro.maru.domain.fair.domain.type.FairStatus;
import com.bamdoliro.maru.domain.fair.domain.type.FairType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class FairDetailResponse {

    private LocalDateTime start;
    private String place;
    private Integer capacity;
    private FairType fairType;
    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;
    private FairStatus status;
    private List<AttendeeResponse> attendeeList;

    public FairDetailResponse(Fair fair, List<Attendee> sortedAttendeeList, Integer headCount) {
        this.start = fair.getStart();
        this.place = fair.getPlace();
        this.capacity = fair.getCapacity();
        this.fairType = fair.getType();
        this.applicationStartDate = fair.getApplicationStartDate();
        this.applicationEndDate = fair.getApplicationEndDate();
        this.status = fair.getStatus(headCount);
        this.attendeeList = sortedAttendeeList.stream()
                .map(AttendeeResponse::new)
                .collect(Collectors.toList());
    }
}
