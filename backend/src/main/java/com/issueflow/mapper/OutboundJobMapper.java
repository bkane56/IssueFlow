package com.issueflow.mapper;

import com.issueflow.dto.response.OutboundJobResponse;
import com.issueflow.entity.OutboundJob;
import org.springframework.stereotype.Component;

@Component
public class OutboundJobMapper {

    public OutboundJobResponse toResponse(OutboundJob job) {
        return new OutboundJobResponse(
                job.getId(),
                job.getOperationType(),
                job.getIdempotencyKey(),
                job.getStatus(),
                job.getAttemptCount(),
                job.getNextAttemptAt(),
                job.getLastHttpStatus(),
                job.getLastError(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getCompletedAt()
        );
    }
}
