package com.issueflow.dto.response;

import com.issueflow.entity.Category;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;

import java.time.Instant;

public record IssueResponse(
        Long id,
        String title,
        String description,
        Category category,
        Severity severity,
        Priority priority,
        int priorityScore,
        IssueStatus status,
        UserResponse assignedUser,
        boolean customerFacing,
        boolean productionImpact,
        int affectedUsers,
        Instant createdAt,
        Instant updatedAt,
        TriageResultResponse triage
) {
}
