package com.bamdoliro.maru.infrastructure.persistence.fair;

import com.bamdoliro.maru.domain.fair.domain.Attendee;
import com.bamdoliro.maru.domain.fair.domain.Fair;

import java.util.List;

public interface AttendeeRepositoryCustom {

    List<Attendee> findByAttendeeIdList(List<Long> idList);

    Integer sumHeadcountByFair(Fair fair);
}
