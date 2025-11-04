package com.bamdoliro.maru.application.fair;

import com.bamdoliro.maru.domain.fair.domain.Attendee;
import com.bamdoliro.maru.domain.fair.domain.Fair;
import com.bamdoliro.maru.domain.fair.exception.AttendeeNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.fair.AttendeeRepository;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class DeleteAttendeeUseCase {

    private final FairFacade fairFacade;
    private final AttendeeRepository attendeeRepository;

    @Transactional
    public void execute(Long fairId, Long attendeeId) {
        Fair fair = fairFacade.getFair(fairId);
        Attendee attendee = getAttendee(attendeeId);
        validateAttendeeInFair(fair, attendee);

        attendeeRepository.delete(attendee);
    }

    private Attendee getAttendee(Long attendeeId) {
        return attendeeRepository.findById(attendeeId)
                .orElseThrow(AttendeeNotFoundException::new);
    }

    private void validateAttendeeInFair(Fair fair, Attendee attendee) {
        if (!attendee.getFair().getId().equals(fair.getId())) {
            throw new AttendeeNotFoundException();
        }
    }
}
