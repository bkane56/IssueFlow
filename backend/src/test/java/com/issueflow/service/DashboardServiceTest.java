package com.issueflow.service;

import com.issueflow.dto.response.DashboardResponse;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Severity;
import com.issueflow.repository.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private IssueRepository issueRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(issueRepository);
    }

    @Test
    void countsOpenCriticalInProgressAndResolved() {
        EnumSet<IssueStatus> closedStatuses = EnumSet.of(IssueStatus.RESOLVED, IssueStatus.CLOSED);
        when(issueRepository.countByStatusNotIn(closedStatuses)).thenReturn(12L);
        when(issueRepository.countBySeverityAndStatusNotIn(Severity.CRITICAL, closedStatuses)).thenReturn(2L);
        when(issueRepository.countByStatus(IssueStatus.IN_PROGRESS)).thenReturn(5L);
        when(issueRepository.countByStatus(IssueStatus.RESOLVED)).thenReturn(18L);

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.open()).isEqualTo(12L);
        assertThat(response.critical()).isEqualTo(2L);
        assertThat(response.inProgress()).isEqualTo(5L);
        assertThat(response.resolved()).isEqualTo(18L);
    }
}
