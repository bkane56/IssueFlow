package com.issueflow.dto.response;

public record UserResponse(
        Long id,
        String name,
        String email,
        boolean active
) {
}
