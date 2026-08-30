package com.issueflow.dto.request;

import com.issueflow.constants.ValidationConstants;
import com.issueflow.entity.Category;
import com.issueflow.entity.Severity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateIssueRequest(
        @NotBlank(message = ValidationConstants.TITLE_REQUIRED)
        @Size(max = ValidationConstants.TITLE_MAX_LENGTH)
        String title,

        @NotBlank(message = ValidationConstants.DESCRIPTION_REQUIRED)
        @Size(max = ValidationConstants.DESCRIPTION_MAX_LENGTH)
        String description,

        @NotNull(message = ValidationConstants.CATEGORY_REQUIRED)
        Category category,

        @NotNull(message = ValidationConstants.SEVERITY_REQUIRED)
        Severity severity,

        Long assignedUserId,

        boolean customerFacing,

        boolean productionImpact,

        @Min(value = 0, message = ValidationConstants.AFFECTED_USERS_MIN)
        int affectedUsers
) {
}
