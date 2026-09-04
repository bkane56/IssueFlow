package com.issueflow.service;

import com.issueflow.constants.OutboundConstants;
import com.issueflow.constants.RetryClassificationConstants;
import com.issueflow.dto.request.CreateIssueRequest;
import com.issueflow.entity.Category;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.IssueHistory;
import com.issueflow.entity.OutboundJob;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.entity.Severity;
import com.issueflow.exception.OutboundTimeoutException;
import com.issueflow.repository.IssueHistoryRepository;
import com.issueflow.repository.OutboundJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class OutboundWorkerServiceTest {

    @Autowired
    private IssueService issueService;

    @Autowired
    private OutboundNotificationService outboundNotificationService;

    @Autowired
    private OutboundWorkerService outboundWorkerService;

    @Autowired
    private OutboundJobRepository outboundJobRepository;

    @Autowired
    private IssueHistoryRepository issueHistoryRepository;

    @MockitoBean
    private ExternalEscalationClient externalEscalationClient;

    @Test
    void processesDueJobUntilSuccess() {
        stubResponses(
                SimulatedHttpResponse.http(RetryClassificationConstants.HTTP_503, OutboundConstants.SIMULATED_HTTP_503),
                SimulatedHttpResponse.success(true)
        );
        Long issueId = createIssue("Worker success after retry");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();

        outboundWorkerService.processDueJobs();
        OutboundJob afterFirst = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(afterFirst.getStatus()).isEqualTo(OutboundJobStatus.RETRY_SCHEDULED);
        assertThat(afterFirst.getAttemptCount()).isEqualTo(1);
        assertThat(afterFirst.getLastHttpStatus()).isEqualTo(RetryClassificationConstants.HTTP_503);
        assertThat(Duration.between(afterFirst.getLastAttemptAt(), afterFirst.getNextAttemptAt()))
                .isEqualTo(Duration.ofSeconds(5));

        outboundWorkerService.processDueJobs();
        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(OutboundJobStatus.RETRY_SCHEDULED);
        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getAttemptCount()).isEqualTo(1);

        makeDue(jobId);
        outboundWorkerService.processDueJobs();
        OutboundJob succeeded = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(succeeded.getStatus()).isEqualTo(OutboundJobStatus.SUCCEEDED);
        assertThat(succeeded.getAttemptCount()).isEqualTo(2);
        assertThat(succeeded.getCompletedAt()).isNotNull();
        assertThat(historyTypes(issueId)).contains(
                HistoryEventType.ESCALATION_NOTIFICATION_QUEUED,
                HistoryEventType.ESCALATION_NOTIFICATION_ATTEMPT_FAILED,
                HistoryEventType.ESCALATION_NOTIFICATION_RETRY_SCHEDULED,
                HistoryEventType.ESCALATION_NOTIFICATION_SUCCEEDED
        );
    }

    @Test
    void ignoresJobsWhoseNextAttemptIsInTheFuture() {
        stubResponses(SimulatedHttpResponse.success(true));
        Long issueId = createIssue("Worker ignores future retry");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();
        OutboundJob job = outboundJobRepository.findById(jobId).orElseThrow();
        job.setNextAttemptAt(Instant.now().plusSeconds(120));
        outboundJobRepository.saveAndFlush(job);

        outboundWorkerService.processDueJobs();

        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(OutboundJobStatus.PENDING);
        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getAttemptCount()).isZero();
    }

    @Test
    void failsImmediatelyOnNonRetryable400() {
        stubResponses(SimulatedHttpResponse.http(RetryClassificationConstants.HTTP_400, OutboundConstants.SIMULATED_HTTP_400));
        Long issueId = createIssue("Worker non-retryable failure");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();

        outboundWorkerService.processDueJobs();

        OutboundJob failed = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(OutboundJobStatus.FAILED);
        assertThat(failed.getAttemptCount()).isEqualTo(1);
        assertThat(failed.getCompletedAt()).isNotNull();
        assertThat(historyTypes(issueId)).contains(
                HistoryEventType.ESCALATION_NOTIFICATION_ATTEMPT_FAILED,
                HistoryEventType.ESCALATION_NOTIFICATION_FAILED
        );
        assertThat(historyTypes(issueId)).doesNotContain(HistoryEventType.ESCALATION_NOTIFICATION_RETRY_SCHEDULED);
    }

    @Test
    void retriesTimeoutAnd408And5xx() {
        stubResponses(
                SimulatedHttpResponse.http(RetryClassificationConstants.HTTP_408, "Request timeout"),
                SimulatedHttpResponse.http(RetryClassificationConstants.HTTP_500, "Server error"),
                SimulatedHttpResponse.success(true)
        );
        Long issueId = createIssue("Worker retryable status codes");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();

        outboundWorkerService.processDueJobs();
        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getLastHttpStatus())
                .isEqualTo(RetryClassificationConstants.HTTP_408);

        makeDue(jobId);
        outboundWorkerService.processDueJobs();
        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getLastHttpStatus())
                .isEqualTo(RetryClassificationConstants.HTTP_500);

        makeDue(jobId);
        outboundWorkerService.processDueJobs();
        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(OutboundJobStatus.SUCCEEDED);
    }

    @Test
    void honorsRetryAfterOn429() {
        stubResponses(SimulatedHttpResponse.rateLimited(30, OutboundConstants.SIMULATED_HTTP_429));
        Long issueId = createIssue("Worker retry after");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();

        outboundWorkerService.processDueJobs();

        OutboundJob job = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(OutboundJobStatus.RETRY_SCHEDULED);
        assertThat(job.getLastHttpStatus()).isEqualTo(RetryClassificationConstants.HTTP_429);
        assertThat(Duration.between(job.getLastAttemptAt(), job.getNextAttemptAt()))
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void marksFailedWhenRetryAttemptsAreExhausted() {
        when(externalEscalationClient.notifyEscalation(anyString(), anyLong()))
                .thenReturn(SimulatedHttpResponse.http(RetryClassificationConstants.HTTP_503, OutboundConstants.SIMULATED_HTTP_503));
        Long issueId = createIssue("Worker retry exhaustion");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();

        for (int attempt = 1; attempt <= 5; attempt++) {
            outboundWorkerService.processDueJobs();
            if (attempt < 5) {
                makeDue(jobId);
            }
        }

        OutboundJob failed = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(OutboundJobStatus.FAILED);
        assertThat(failed.getAttemptCount()).isEqualTo(5);
        assertThat(historyTypes(issueId)).contains(HistoryEventType.ESCALATION_NOTIFICATION_FAILED);
    }

    @Test
    void treatsTimeoutAsRetryableFailure() {
        when(externalEscalationClient.notifyEscalation(anyString(), anyLong()))
                .thenThrow(new OutboundTimeoutException(OutboundConstants.SIMULATED_TIMEOUT));
        Long issueId = createIssue("Worker timeout retry");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();

        outboundWorkerService.processDueJobs();

        OutboundJob job = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(OutboundJobStatus.RETRY_SCHEDULED);
        assertThat(job.getLastHttpStatus()).isNull();
        assertThat(job.getLastError()).isEqualTo(OutboundConstants.SIMULATED_TIMEOUT);
    }

    @Test
    void persistsRetryStateAcrossPersistenceContextClear() {
        stubResponses(
                SimulatedHttpResponse.http(RetryClassificationConstants.HTTP_503, OutboundConstants.SIMULATED_HTTP_503),
                SimulatedHttpResponse.success(true)
        );
        Long issueId = createIssue("Worker restart durability");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();
        outboundWorkerService.processDueJobs();

        OutboundJob persisted = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OutboundJobStatus.RETRY_SCHEDULED);
        assertThat(persisted.getIdempotencyKey()).isEqualTo("ESCALATION_NOTIFICATION:" + issueId);
        assertThat(persisted.getAttemptCount()).isEqualTo(1);

        makeDue(jobId);
        outboundWorkerService.processDueJobs();
        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(OutboundJobStatus.SUCCEEDED);
    }

    @Test
    void reclaimsStaleProcessingJobs() {
        stubResponses(SimulatedHttpResponse.success(true));
        Long issueId = createIssue("Worker stale processing reclaim");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();
        OutboundJob job = outboundJobRepository.findById(jobId).orElseThrow();
        job.setStatus(OutboundJobStatus.PROCESSING);
        job.setUpdatedAt(Instant.now().minusSeconds(31));
        outboundJobRepository.saveAndFlush(job);

        outboundWorkerService.processDueJobs();

        OutboundJob reclaimed = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(reclaimed.getStatus()).isEqualTo(OutboundJobStatus.SUCCEEDED);
        assertThat(reclaimed.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void failsImmediatelyOn409() {
        stubResponses(SimulatedHttpResponse.http(RetryClassificationConstants.HTTP_409, "Conflict"));
        Long issueId = createIssue("Worker non-retryable conflict");
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();

        outboundWorkerService.processDueJobs();

        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(OutboundJobStatus.FAILED);
        assertThat(outboundJobRepository.findById(jobId).orElseThrow().getAttemptCount()).isEqualTo(1);
    }

    private void stubResponses(SimulatedHttpResponse first, SimulatedHttpResponse... rest) {
        when(externalEscalationClient.notifyEscalation(anyString(), anyLong())).thenReturn(first, rest);
    }

    private void makeDue(Long jobId) {
        OutboundJob job = outboundJobRepository.findById(jobId).orElseThrow();
        job.setNextAttemptAt(Instant.now().minusSeconds(1));
        outboundJobRepository.saveAndFlush(job);
    }

    private Long createIssue(String title) {
        return issueService.create(new CreateIssueRequest(
                title,
                "Used to exercise outbound worker retry behavior.",
                Category.INTEGRATION,
                Severity.HIGH,
                null,
                true,
                true,
                25
        )).id();
    }

    private List<HistoryEventType> historyTypes(Long issueId) {
        return issueHistoryRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .map(IssueHistory::getEventType)
                .toList();
    }
}
