package com.issueflow.service;

import com.issueflow.constants.ErrorConstants;
import com.issueflow.constants.OutboundConstants;
import com.issueflow.dto.response.OutboundJobResponse;
import com.issueflow.entity.Category;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.OutboundJob;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.entity.OutboundOperationType;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import com.issueflow.exception.InvalidStateTransitionException;
import com.issueflow.exception.ResourceNotFoundException;
import com.issueflow.mapper.OutboundJobMapper;
import com.issueflow.repository.IssueRepository;
import com.issueflow.repository.OutboundJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundNotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-04T12:00:00Z");

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private OutboundJobRepository outboundJobRepository;

    private OutboundNotificationService outboundNotificationService;

    @BeforeEach
    void setUp() {
        outboundNotificationService = new OutboundNotificationService(
                issueRepository,
                outboundJobRepository,
                new OutboundJobMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void enqueueCreatesPendingJobAndIssueHistory() {
        Issue issue = existingIssue(IssueStatus.IN_PROGRESS);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboundJobRepository.findByIdempotencyKey("ESCALATION_NOTIFICATION:10")).thenReturn(Optional.empty());
        when(outboundJobRepository.save(any(OutboundJob.class))).thenAnswer(invocation -> {
            OutboundJob job = invocation.getArgument(0);
            job.setId(21L);
            return job;
        });

        OutboundJobResponse response = outboundNotificationService.enqueueEscalation(10L);

        assertThat(response.jobId()).isEqualTo(21L);
        assertThat(response.operationType()).isEqualTo(OutboundOperationType.ESCALATION_NOTIFICATION);
        assertThat(response.idempotencyKey()).isEqualTo("ESCALATION_NOTIFICATION:10");
        assertThat(response.status()).isEqualTo(OutboundJobStatus.PENDING);
        assertThat(response.attemptCount()).isZero();
        assertThat(response.nextAttemptAt()).isEqualTo(NOW);
        assertThat(issue.getEscalationRequestedAt()).isEqualTo(NOW);
        assertThat(issue.getHistory()).extracting(history -> history.getEventType())
                .containsExactly(HistoryEventType.ESCALATION_NOTIFICATION_QUEUED);
        assertThat(issue.getHistory().get(0).getDescription()).isEqualTo(OutboundConstants.HISTORY_QUEUED);
        assertThat(issue.getHistory().get(0).getNewValue()).isEqualTo("ESCALATION_NOTIFICATION:10");
    }

    @Test
    void enqueueReturnsExistingJobWithoutCreatingAnother() {
        Issue issue = existingIssue(IssueStatus.IN_PROGRESS);
        OutboundJob existing = existingJob(issue);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
        when(outboundJobRepository.findByIdempotencyKey("ESCALATION_NOTIFICATION:10")).thenReturn(Optional.of(existing));

        OutboundJobResponse first = outboundNotificationService.enqueueEscalation(10L);
        OutboundJobResponse second = outboundNotificationService.enqueueEscalation(10L);

        assertThat(first.jobId()).isEqualTo(21L);
        assertThat(second.jobId()).isEqualTo(21L);
        assertThat(second.idempotencyKey()).isEqualTo(first.idempotencyKey());
        verify(outboundJobRepository, never()).save(any(OutboundJob.class));
        assertThat(issue.getHistory()).isEmpty();
    }

    @Test
    void enqueueRejectsMissingIssue() {
        when(issueRepository.findById(1042L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outboundNotificationService.enqueueEscalation(1042L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorConstants.ISSUE_NOT_FOUND.formatted(1042L));
        verify(outboundJobRepository, never()).save(any(OutboundJob.class));
    }

    @Test
    void enqueueRejectsClosedIssue() {
        Issue issue = existingIssue(IssueStatus.CLOSED);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> outboundNotificationService.enqueueEscalation(10L))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage(ErrorConstants.ESCALATION_NOT_ALLOWED_FOR_CLOSED_ISSUE);
        verify(outboundJobRepository, never()).save(any(OutboundJob.class));
    }

    @Test
    void findByIssueIdReturnsJobsForExistingIssue() {
        Issue issue = existingIssue(IssueStatus.IN_PROGRESS);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
        when(outboundJobRepository.findByIssueIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(existingJob(issue)));

        List<OutboundJobResponse> jobs = outboundNotificationService.findByIssueId(10L);

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).jobId()).isEqualTo(21L);
    }

    @Test
    void findByIdReturnsJob() {
        Issue issue = existingIssue(IssueStatus.IN_PROGRESS);
        when(outboundJobRepository.findById(21L)).thenReturn(Optional.of(existingJob(issue)));

        OutboundJobResponse response = outboundNotificationService.findById(21L);

        assertThat(response.jobId()).isEqualTo(21L);
        assertThat(response.idempotencyKey()).isEqualTo("ESCALATION_NOTIFICATION:10");
    }

    @Test
    void findByIdRejectsMissingJob() {
        when(outboundJobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outboundNotificationService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorConstants.OUTBOUND_JOB_NOT_FOUND.formatted(99L));
    }

    private Issue existingIssue(IssueStatus status) {
        Issue issue = new Issue();
        issue.setId(10L);
        issue.setTitle("Payment confirmation timeout");
        issue.setDescription("Checkout stalls when the payment provider is slow.");
        issue.setCategory(Category.INTEGRATION);
        issue.setSeverity(Severity.HIGH);
        issue.setPriority(Priority.P2);
        issue.setPriorityScore(75);
        issue.setStatus(status);
        issue.setCustomerFacing(true);
        issue.setProductionImpact(true);
        issue.setAffectedUsers(40);
        issue.setCreatedAt(NOW);
        issue.setUpdatedAt(NOW);
        return issue;
    }

    private OutboundJob existingJob(Issue issue) {
        OutboundJob job = new OutboundJob();
        job.setId(21L);
        job.setOperationType(OutboundOperationType.ESCALATION_NOTIFICATION);
        job.setIssue(issue);
        job.setIdempotencyKey(OutboundOperationType.ESCALATION_NOTIFICATION.idempotencyKey(issue.getId()));
        job.setStatus(OutboundJobStatus.PENDING);
        job.setAttemptCount(0);
        job.setNextAttemptAt(NOW);
        job.setCreatedAt(NOW);
        job.setUpdatedAt(NOW);
        return job;
    }
}
