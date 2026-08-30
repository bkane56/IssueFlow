package com.issueflow.mapper;

import com.issueflow.dto.response.IssueResponse;
import com.issueflow.entity.Issue;
import com.issueflow.service.TriageResult;
import org.springframework.stereotype.Component;

@Component
public class IssueMapper {

    private final UserMapper userMapper;
    private final TriageMapper triageMapper;

    public IssueMapper(UserMapper userMapper, TriageMapper triageMapper) {
        this.userMapper = userMapper;
        this.triageMapper = triageMapper;
    }

    public IssueResponse toResponse(Issue issue, TriageResult triageResult) {
        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getCategory(),
                issue.getSeverity(),
                issue.getPriority(),
                issue.getPriorityScore(),
                issue.getStatus(),
                userMapper.toResponse(issue.getAssignedUser()),
                issue.isCustomerFacing(),
                issue.isProductionImpact(),
                issue.getAffectedUsers(),
                issue.getCreatedAt(),
                issue.getUpdatedAt(),
                triageMapper.toResponse(triageResult)
        );
    }
}
