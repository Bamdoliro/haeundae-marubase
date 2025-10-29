package com.bamdoliro.maru.application.fair;

import com.bamdoliro.maru.domain.fair.domain.Fair;
import com.bamdoliro.maru.domain.fair.exception.FairNotFoundException;
import com.bamdoliro.maru.presentation.fair.dto.request.UpdateFairRequest;
import com.bamdoliro.maru.shared.fixture.FairFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UpdateAdmissionFairUseCaseTest {

    @InjectMocks
    private UpdateAdmissionFairUseCase updateAdmissionFairUseCase;

    @Mock
    private FairFacade fairFacade;

    @Test
    void 유저가_입학설명회를_수정한다() {
        //given
        Fair fair = FairFixture.createFair();
        UpdateFairRequest request = FairFixture.updateFairRequest();

        given(fairFacade.getFair(fair.getId())).willReturn(fair);

        //when
        updateAdmissionFairUseCase.execute(fair.getId(), request);

        // then
        verify(fairFacade, times(1)).getFair(fair.getId());
        assertEquals(request.getStart(), fair.getStart());
        assertEquals(request.getCapacity(), fair.getCapacity());
        assertEquals(request.getPlace(), fair.getPlace());
        assertEquals(request.getType(), fair.getType());
        assertEquals(request.getApplicationStartDate(), fair.getApplicationStartDate());
        assertEquals(request.getApplicationEndDate(), fair.getApplicationEndDate());
    }

    @Test
    void 유저가_입학설명회를_수정할_때_입학설명회가_없으면_에러가_발생한다() {
        // given
        Fair fair = FairFixture.createFair();
        UpdateFairRequest request = FairFixture.updateFairRequest();

        given(fairFacade.getFair(fair.getId())).willThrow(FairNotFoundException.class);

        // when and then
        assertThrows(FairNotFoundException.class, () -> updateAdmissionFairUseCase.execute(fair.getId(), request));

        verify(fairFacade, times(1)).getFair(fair.getId());
        assertNotEquals(request.getStart(), fair.getStart());
        assertNotEquals(request.getCapacity(), fair.getCapacity());
        assertNotEquals(request.getPlace(), fair.getPlace());
        assertNotEquals(request.getType(), fair.getType());
        assertNotEquals(request.getApplicationStartDate(), fair.getApplicationStartDate());
        assertNotEquals(request.getApplicationEndDate(), fair.getApplicationEndDate());
    }
}
