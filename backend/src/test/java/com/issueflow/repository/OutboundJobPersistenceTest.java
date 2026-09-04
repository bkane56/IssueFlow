package com.issueflow.repository;

import com.issueflow.entity.Category;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.OutboundJob;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.entity.OutboundOperationType;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboundJobPersistenceTest {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private OutboundJobRepository outboundJobRepository;

    @Test
    void savesAndReloadsOutboundJobWithUniqueIdempotencyKey() {
        Instant now = Instant.parse("2026-09-04T12:00:00Z");
        Issue issue = issueRepository.saveAndFlush(newIssue(now));

        OutboundJob job = new OutboundJob();
        job.setOperationType(OutboundOperationType.ESCALATION_NOTIFICATION);
        job.setIssue(issue);
        job.setIdempotencyKey(OutboundOperationType.ESCALATION_NOTIFICATION.idempotencyKey(issue.getId()));
        job.setStatus(OutboundJobStatus.PENDING);
        job.setAttemptCount(0);
        job.setNextAttemptAt(now);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        OutboundJob saved = outboundJobRepository.saveAndFlush(job);
        outboundJobRepository.flush();

        OutboundJob loaded = outboundJobRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getIdempotencyKey()).isEqualTo("ESCALATION_NOTIFICATION:" + issue.getId());
        assertThat(loaded.getStatus()).isEqualTo(OutboundJobStatus.PENDING);
        assertThat(loaded.getIssue().getId()).isEqualTo(issue.getId());
        assertThat(loaded.getAttemptCount()).isZero();
        assertThat(outboundJobRepository.findByIdempotencyKey(loaded.getIdempotencyKey())).isPresent();
        assertThat(outboundJobRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId())).hasSize(1);
    }

    @Test
    void persistsEscalationRequestedAtOnIssue() {
        Instant now = Instant.parse("2026-09-04T12:00:00Z");
        Issue issue = newIssue(now);
        issue.setEscalationRequestedAt(now);
        Long issueId = issueRepository.saveAndFlush(issue).getId();

        Issue loaded = issueRepository.findById(issueId).orElseThrow();
        assertThat(loaded.getEscalationRequestedAt()).isEqualTo(now);
    }

    private static Issue newIssue(Instant now) {
        Issue issue = new Issue();
        issue.setTitle("Checkout latency after payment gateway timeout");
        issue.setDescription("Customers see a timeout when the payment provider is slow to respond.");
        issue.setCategory(Category.INTEGRATION);
        issue.setSeverity(Severity.HIGH);
        issue.setPriority(Priority.P2);
        issue.setPriorityScore(75);
        issue.setStatus(IssueStatus.IN_PROGRESS);
        issue.setCustomerFacing(true);
        issue.setProductionImpact(true);
        issue.setAffectedUsers(40);
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);
        return issue;
    }
}
