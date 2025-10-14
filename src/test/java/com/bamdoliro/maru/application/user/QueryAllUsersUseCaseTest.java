package com.bamdoliro.maru.application.user;

import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.user.domain.type.Authority;
import com.bamdoliro.maru.domain.user.service.UserFacade;
import com.bamdoliro.maru.presentation.user.dto.response.UserWithFormStatusResponse;
import com.bamdoliro.maru.shared.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueryAllUsersUseCaseTest {

    @InjectMocks
    private QueryAllUsersUseCase queryAllUsersUseCase;

    @Mock
    private UserFacade userFacade;

    @Test
    void 모든_사용자를_원서_제출_여부와_함께_조회한다() {
        // given
        User user1 = UserFixture.createUserWithId(1L);
        User user2 = UserFixture.createUserWithId(2L);
        List<User> users = List.of(user1, user2);

        Set<Long> userIdsWithForm = Set.of();

        given(userFacade.getAllUsersByAuthority(Authority.USER)).willReturn(users);
        given(userFacade.getUserIdsWithForm(List.of(1L, 2L))).willReturn(userIdsWithForm);

        // when
        List<UserWithFormStatusResponse> result = queryAllUsersUseCase.execute();

        // then
        assertNotNull(result);
        assertEquals(2, result.size());

        result.forEach(response -> assertFalse(response.getHasSubmittedForm()));

        verify(userFacade).getAllUsersByAuthority(Authority.USER);
        verify(userFacade).getUserIdsWithForm(List.of(1L, 2L));
    }
}