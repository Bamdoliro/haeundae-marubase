package com.bamdoliro.maru.application.fair;

import com.bamdoliro.maru.domain.fair.domain.Attendee;
import com.bamdoliro.maru.domain.fair.domain.Fair;
import com.bamdoliro.maru.domain.fair.exception.AttendeeNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.fair.AttendeeRepository;
import com.bamdoliro.maru.shared.fixture.FairFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        ReflectionTestUtils.setField(fair, "id", fairId); // ID 설정 추가

        Attendee attendee = FairFixture.createAttendee(fair);

        given(fairFacade.getFair(fairId)).willReturn(fair);
        given(attendeeRepository.findById(attendeeId)).willReturn(Optional.of(attendee));
        willDoNothing().given(attendeeRepository).delete(attendee);

        // when
        deleteAttendeeUseCase.execute(fairId, attendeeId);

        // then
        verify(fairFacade, times(1)).getFair(fairId);
        verify(attendeeRepository, times(1)).findById(attendeeId);
        verify(attendeeRepository, times(1)).delete(attendee);
    }

    @Test
    void 존재하지_않는_신청자_삭제시_예외가_발생한다() throws Exception {
        // given
        Long fairId = 1L;
        Long attendeeId = 1L;

        Fair fair = FairFixture.createFair();

        given(fairFacade.getFair(fairId)).willReturn(fair);
        given(attendeeRepository.findById(attendeeId)).willReturn(Optional.empty());

        // when & then
        assertThrows(AttendeeNotFoundException.class, () -> {
            deleteAttendeeUseCase.execute(fairId, attendeeId);
        });
    }

    @Test
    void 다른_설명회의_신청자_삭제시_예외가_발생한다() throws Exception {
        // given
        Long fairId = 1L;
        Long attendeeId = 1L;

        Fair fair = FairFixture.createFair();
        Fair anotherFair = FairFixture.createAnotherFair();

        ReflectionTestUtils.setField(fair, "id", fairId);
        ReflectionTestUtils.setField(anotherFair, "id", 2L); // 다른 ID

        Attendee attendee = FairFixture.createAttendee(anotherFair);

        given(fairFacade.getFair(fairId)).willReturn(fair);
        given(attendeeRepository.findById(attendeeId)).willReturn(Optional.of(attendee));

        // when & then
        assertThrows(AttendeeNotFoundException.class, () -> {
            deleteAttendeeUseCase.execute(fairId, attendeeId);
        });
    }
}