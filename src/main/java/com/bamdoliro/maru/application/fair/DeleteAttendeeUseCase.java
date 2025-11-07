package com.bamdoliro.maru.application.fair;

import com.bamdoliro.maru.domain.fair.domain.Attendee;
import com.bamdoliro.maru.domain.fair.exception.AttendeeNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.fair.AttendeeRepository;
import com.bamdoliro.maru.presentation.fair.dto.request.DeleteAttendeeListRequest;
import com.bamdoliro.maru.presentation.fair.dto.request.DeleteAttendeeRequest;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@UseCase
public class DeleteAttendeeUseCase {

    private final FairFacade fairFacade;
    private final AttendeeRepository attendeeRepository;

    @Transactional
    public void execute(Long fairId, DeleteAttendeeListRequest request) {
        List<DeleteAttendeeRequest> requestList = getSortedList(request);
        List<Attendee> attendeeList = attendeeRepository.findByAttendeeIdList(
                requestList.stream()
                        .map(DeleteAttendeeRequest::getAttendeeId)
                        .toList()
        );

        for (int i = 0; i < attendeeList.size(); i++) {
            Attendee attendee = attendeeList.get(i);

            validateAttendeeInFair(fairId, attendee);

            attendeeRepository.deleteById(attendee.getId());
        }
    }

    private void validateAttendeeInFair(Long fairId, Attendee attendee) {
        if (!attendee.getFair().getId().equals(fairId)) {
            throw new AttendeeNotFoundException();
        }
    }

    private List<DeleteAttendeeRequest> getSortedList(DeleteAttendeeListRequest request) {
        return request.getAttendeeList().stream()
                .sorted(Comparator.comparingLong(DeleteAttendeeRequest::getAttendeeId))
                .toList();
    }
}
