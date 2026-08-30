package com.issueflow.service;

import com.issueflow.dto.response.DashboardResponse;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Severity;
import com.issueflow.repository.IssueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final EnumSet<IssueStatus> CLOSED_STATUSES = EnumSet.of(
            IssueStatus.RESOLVED,
            IssueStatus.CLOSED
    );

    private final IssueRepository issueRepository;

    public DashboardService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public DashboardResponse getDashboard() {
        long open = issueRepository.countByStatusNotIn(CLOSED_STATUSES);
        long critical = issueRepository.countBySeverityAndStatusNotIn(Severity.CRITICAL, CLOSED_STATUSES);
        long inProgress = issueRepository.countByStatus(IssueStatus.IN_PROGRESS);
        long resolved = issueRepository.countByStatus(IssueStatus.RESOLVED);
        return new DashboardResponse(open, critical, inProgress, resolved);
    }
}
