package com.issueflow.dto.response;

import com.issueflow.entity.HistoryEventType;

import java.time.Instant;

public record IssueHistoryResponse(
        Long id,
        HistoryEventType eventType,
        String oldValue,
        String newValue,
        String description,
        Instant createdAt
) {
}
