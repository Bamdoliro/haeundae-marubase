package com.bamdoliro.maru.infrastructure.persistence.fair;

import com.bamdoliro.maru.domain.fair.domain.Attendee;
import com.bamdoliro.maru.domain.fair.domain.Fair;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.bamdoliro.maru.domain.fair.domain.QAttendee.attendee;

@Repository
@RequiredArgsConstructor
public class AttendeeRepositoryImpl implements AttendeeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Attendee> findByAttendeeIdList(List<Long> idList) {
        return queryFactory
                .selectFrom(attendee)
                .where(attendee.id.in(idList))
                .orderBy(attendee.id.asc())
                .fetch();
    }

    @Override
    public Integer sumHeadcountByFair(Fair fair) {
        Integer sum = queryFactory
                .select(attendee.headcount.sum())
                .from(attendee)
                .where(attendee.fair.eq(fair))
                .fetchOne();

        return sum != null ? sum : 0;
    }
}
