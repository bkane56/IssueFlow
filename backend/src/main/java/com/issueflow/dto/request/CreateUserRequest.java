package com.issueflow.dto.request;

import com.issueflow.constants.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = ValidationConstants.NAME_REQUIRED)
        @Size(max = ValidationConstants.NAME_MAX_LENGTH)
        String name,

        @NotBlank(message = ValidationConstants.EMAIL_REQUIRED)
        @Email(message = ValidationConstants.EMAIL_INVALID)
        @Size(max = ValidationConstants.EMAIL_MAX_LENGTH)
        String email
) {
}
