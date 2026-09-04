package com.issueflow.service;

import com.issueflow.dto.request.CreateIssueRequest;
import com.issueflow.dto.response.IssueResponse;
import com.issueflow.entity.Category;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueHistory;
import com.issueflow.entity.OutboundJob;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.entity.OutboundOperationType;
import com.issueflow.entity.Severity;
import com.issueflow.repository.IssueHistoryRepository;
import com.issueflow.repository.IssueRepository;
import com.issueflow.repository.OutboundJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OutboundNotificationAtomicityTest {

    @Autowired
    private IssueService issueService;

    @Autowired
    private OutboundNotificationService outboundNotificationService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueHistoryRepository issueHistoryRepository;

    @Autowired
    private OutboundJobRepository outboundJobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void persistsIssueStateAndOutboundJobTogether() {
        Long issueId = createOpenIssue("Escalation atomic commit case");

        outboundNotificationService.enqueueEscalation(issueId);

        Issue issue = issueRepository.findById(issueId).orElseThrow();
        assertThat(issue.getEscalationRequestedAt()).isNotNull();

        String idempotencyKey = OutboundOperationType.ESCALATION_NOTIFICATION.idempotencyKey(issueId);
        OutboundJob job = outboundJobRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(OutboundJobStatus.PENDING);
        assertThat(job.getIssue().getId()).isEqualTo(issueId);
        assertThat(job.getAttemptCount()).isZero();

        assertThat(issueHistoryRepository.findByIssueIdOrderByCreatedAtAsc(issueId))
                .extracting(IssueHistory::getEventType)
                .contains(HistoryEventType.ESCALATION_NOTIFICATION_QUEUED);

        outboundNotificationService.enqueueEscalation(issueId);
        assertThat(outboundJobRepository.findByIssueIdOrderByCreatedAtAsc(issueId)).hasSize(1);
    }

    @Test
    void rollsBackIssueStateAndOutboundJobWhenTransactionFails() {
        Long issueId = createOpenIssue("Escalation atomic rollback case");
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            outboundNotificationService.enqueueEscalation(issueId);
            throw new IllegalStateException("forced rollback");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("forced rollback");

        Issue issue = issueRepository.findById(issueId).orElseThrow();
        assertThat(issue.getEscalationRequestedAt()).isNull();
        assertThat(outboundJobRepository.findByIssueIdOrderByCreatedAtAsc(issueId)).isEmpty();
        assertThat(issueHistoryRepository.findByIssueIdOrderByCreatedAtAsc(issueId))
                .extracting(IssueHistory::getEventType)
                .doesNotContain(HistoryEventType.ESCALATION_NOTIFICATION_QUEUED);
    }

    private Long createOpenIssue(String title) {
        IssueResponse created = issueService.create(new CreateIssueRequest(
                title,
                "Used to verify that outbound intent and issue state share one transaction.",
                Category.INTEGRATION,
                Severity.HIGH,
                null,
                true,
                true,
                25
        ));
        return created.id();
    }
}
