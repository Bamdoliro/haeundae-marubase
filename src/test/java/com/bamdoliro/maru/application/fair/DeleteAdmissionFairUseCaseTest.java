package com.bamdoliro.maru.application.fair;

import com.bamdoliro.maru.infrastructure.persistence.fair.FairRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DeleteAdmissionFairUseCaseTest {

    @InjectMocks
    private DeleteAdmissionFairUseCase deleteAdmissionFairUseCase;

    @Mock
    private FairRepository fairRepository;

    @Test
    void 입학설명회_일정을_삭제한다() throws Exception {
        //given
        Long id = 1L;
        willDoNothing().given(fairRepository).deleteById(id);

        // when
        deleteAdmissionFairUseCase.execute(id);

        // then
        verify(fairRepository, times(1)).deleteById(id);
    }
}
