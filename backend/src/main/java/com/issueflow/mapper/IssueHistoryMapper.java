package com.issueflow.mapper;

import com.issueflow.dto.response.IssueHistoryResponse;
import com.issueflow.entity.IssueHistory;
import org.springframework.stereotype.Component;

@Component
public class IssueHistoryMapper {

    public IssueHistoryResponse toResponse(IssueHistory history) {
        return new IssueHistoryResponse(
                history.getId(),
                history.getEventType(),
                history.getOldValue(),
                history.getNewValue(),
                history.getDescription(),
                history.getCreatedAt()
        );
    }
}
