package com.bamdoliro.maru.application.user;

import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.user.domain.type.Authority;
import com.bamdoliro.maru.domain.user.service.UserFacade;
import com.bamdoliro.maru.presentation.user.dto.response.UserWithFormStatusResponse;
import com.bamdoliro.maru.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@UseCase
public class QueryAllUsersUseCase {

    private final UserFacade userFacade;

    public List<UserWithFormStatusResponse> execute() {
        List<User> users = userFacade.getAllUsersByAuthority(Authority.USER);

        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        Set<Long> userIdsWithForm = userFacade.getUserIdsWithForm(userIds);

        return users.stream()
                .map(user -> new UserWithFormStatusResponse(
                    user,
                    userIdsWithForm.contains(user.getId())
                ))
                .toList();
    }
}