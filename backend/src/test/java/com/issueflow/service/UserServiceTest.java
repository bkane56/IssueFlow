package com.issueflow.service;

import com.issueflow.constants.ErrorConstants;
import com.issueflow.dto.request.CreateUserRequest;
import com.issueflow.dto.response.UserResponse;
import com.issueflow.entity.User;
import com.issueflow.exception.DuplicateResourceException;
import com.issueflow.mapper.UserMapper;
import com.issueflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, new UserMapper());
    }

    @Test
    void createSavesActiveAssignee() {
        when(userRepository.existsByEmailIgnoreCase("casey.nguyen@issueflow.local")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(8L);
            return user;
        });

        UserResponse response = userService.create(new CreateUserRequest(
                "  Casey Nguyen  ",
                "  casey.nguyen@issueflow.local  "
        ));

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.name()).isEqualTo("Casey Nguyen");
        assertThat(response.email()).isEqualTo("casey.nguyen@issueflow.local");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("alex.chen@issueflow.local")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new CreateUserRequest(
                "Alex Chen",
                "alex.chen@issueflow.local"
        )))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(ErrorConstants.EMAIL_IN_USE);
        verify(userRepository, never()).save(any(User.class));
    }
}
