package com.issueflow.dto.response;

import com.issueflow.entity.Priority;

public record PriorityChangeResponse(
        Priority previousPriority,
        Priority currentPriority,
        boolean changed,
        IssueResponse issue
) {
}
