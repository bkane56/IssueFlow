package com.issueflow.mapper;

import com.issueflow.dto.response.UserResponse;
import com.issueflow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.isActive());
    }
}
