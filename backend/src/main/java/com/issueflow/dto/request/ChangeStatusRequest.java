package com.issueflow.dto.request;

import com.issueflow.constants.ValidationConstants;
import com.issueflow.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull(message = ValidationConstants.STATUS_REQUIRED)
        IssueStatus status
) {
}
