package com.issueflow.dto.response;

import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.entity.OutboundOperationType;

import java.time.Instant;

public record OutboundJobResponse(
        Long jobId,
        OutboundOperationType operationType,
        String idempotencyKey,
        OutboundJobStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        Integer lastHttpStatus,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}
