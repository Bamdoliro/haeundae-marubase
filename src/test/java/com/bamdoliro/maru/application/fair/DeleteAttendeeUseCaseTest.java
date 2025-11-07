package com.bamdoliro.maru.application.fair;

import com.bamdoliro.maru.domain.fair.domain.Attendee;
import com.bamdoliro.maru.domain.fair.domain.Fair;
import com.bamdoliro.maru.domain.fair.exception.AttendeeNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.fair.AttendeeRepository;
import com.bamdoliro.maru.presentation.fair.dto.request.DeleteAttendeeListRequest;
import com.bamdoliro.maru.presentation.fair.dto.request.DeleteAttendeeRequest;
import com.bamdoliro.maru.shared.fixture.FairFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteAttendeeUseCaseTest {

    @InjectMocks
    private DeleteAttendeeUseCase deleteAttendeeUseCase;

    @Mock
    private FairFacade fairFacade;

    @Mock
    private AttendeeRepository attendeeRepository;

    @Test
    void 신청자를_삭제한다() throws Exception {
        // given
        Long fairId = 1L;
        Long attendeeId = 1L;

        Fair fair = FairFixture.createFair();
        ReflectionTestUtils.setField(fair, "id", fairId);

        Attendee attendee = FairFixture.createAttendee(fair);
        ReflectionTestUtils.setField(attendee, "id", attendeeId);

        DeleteAttendeeRequest deleteAttendeeRequest = new DeleteAttendeeRequest(attendeeId);
        DeleteAttendeeListRequest request = new DeleteAttendeeListRequest(List.of(deleteAttendeeRequest));

        given(attendeeRepository.findByAttendeeIdList(List.of(attendeeId))).willReturn(List.of(attendee));
        willDoNothing().given(attendeeRepository).deleteById(attendeeId);

        // when
        deleteAttendeeUseCase.execute(fairId, request);

        // then
        verify(attendeeRepository, times(1)).findByAttendeeIdList(List.of(attendeeId));
        verify(attendeeRepository, times(1)).deleteById(attendeeId);
    }

    @Test
    void 존재하지_않는_신청자_삭제시_아무_일도_일어나지_않는다() throws Exception {
        // given
        Long fairId = 1L;
        Long attendeeId = 1L;

        DeleteAttendeeRequest deleteAttendeeRequest = new DeleteAttendeeRequest(attendeeId);
        DeleteAttendeeListRequest request = new DeleteAttendeeListRequest(List.of(deleteAttendeeRequest));

        given(attendeeRepository.findByAttendeeIdList(List.of(attendeeId))).willReturn(List.of());

        // when & then
        deleteAttendeeUseCase.execute(fairId, request);

        verify(attendeeRepository, times(1)).findByAttendeeIdList(List.of(attendeeId));
        verify(attendeeRepository, never()).deleteById(any());
    }

    @Test
    void 다른_설명회의_신청자_삭제시_예외가_발생한다() throws Exception {
        // given
        Long fairId = 1L;
        Long attendeeId = 1L;

        Fair fair = FairFixture.createFair();
        Fair anotherFair = FairFixture.createAnotherFair();

        ReflectionTestUtils.setField(fair, "id", fairId);
        ReflectionTestUtils.setField(anotherFair, "id", 2L);

        Attendee attendee = FairFixture.createAttendee(anotherFair);
        ReflectionTestUtils.setField(attendee, "id", attendeeId);

        DeleteAttendeeRequest deleteAttendeeRequest = new DeleteAttendeeRequest(attendeeId);
        DeleteAttendeeListRequest request = new DeleteAttendeeListRequest(List.of(deleteAttendeeRequest));

        given(attendeeRepository.findByAttendeeIdList(List.of(attendeeId))).willReturn(List.of(attendee));

        // when & then
        assertThrows(AttendeeNotFoundException.class, () -> {
            deleteAttendeeUseCase.execute(fairId, request);
        });
    }
}