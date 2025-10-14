package com.bamdoliro.maru.domain.user.service;

import com.bamdoliro.maru.domain.user.domain.User;
import com.bamdoliro.maru.domain.user.domain.type.Authority;
import com.bamdoliro.maru.domain.user.exception.UserNotFoundException;
import com.bamdoliro.maru.infrastructure.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(UserNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsersByAuthority(Authority authority) {
        return userRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public Set<Long> getUserIdsWithForm(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(userRepository.findUserIdsWithForm(userIds));
    }
}
