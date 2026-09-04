package com.issueflow.service;

import com.issueflow.config.OutboundProperties;
import com.issueflow.constants.OutboundConstants;
import com.issueflow.constants.RetryClassificationConstants;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueHistory;
import com.issueflow.entity.OutboundJob;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.exception.OutboundTimeoutException;
import com.issueflow.repository.IssueRepository;
import com.issueflow.repository.OutboundJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Service
public class OutboundWorkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundWorkerService.class);
    private static final List<OutboundJobStatus> CLAIMABLE_STATUSES = List.of(
            OutboundJobStatus.PENDING,
            OutboundJobStatus.RETRY_SCHEDULED
    );

    private final OutboundJobRepository outboundJobRepository;
    private final IssueRepository issueRepository;
    private final ExternalEscalationClient externalEscalationClient;
    private final RetryClassifier retryClassifier;
    private final RetryPolicy retryPolicy;
    private final OutboundProperties properties;
    private final TransactionTemplate transactionTemplate;

    public OutboundWorkerService(
            OutboundJobRepository outboundJobRepository,
            IssueRepository issueRepository,
            ExternalEscalationClient externalEscalationClient,
            RetryClassifier retryClassifier,
            RetryPolicy retryPolicy,
            OutboundProperties properties,
            PlatformTransactionManager transactionManager
    ) {
        this.outboundJobRepository = outboundJobRepository;
        this.issueRepository = issueRepository;
        this.externalEscalationClient = externalEscalationClient;
        this.retryClassifier = retryClassifier;
        this.retryPolicy = retryPolicy;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${issueflow.outbound.worker.interval-ms:2000}")
    public void processDueJobsOnSchedule() {
        if (!properties.getWorker().isEnabled()) {
            return;
        }
        processDueJobs();
    }

    public void processDueJobs() {
        Instant now = retryPolicy.now();
        transactionTemplate.executeWithoutResult(status -> reclaimStaleJobs(now));
        List<Long> dueJobIds = transactionTemplate.execute(status -> outboundJobRepository
                .findByStatusInAndNextAttemptAtLessThanEqual(CLAIMABLE_STATUSES, now)
                .stream()
                .map(OutboundJob::getId)
                .toList());
        if (dueJobIds == null || dueJobIds.isEmpty()) {
            return;
        }
        for (Long jobId : dueJobIds) {
            processJob(jobId);
        }
    }

    private void processJob(Long jobId) {
        ClaimedOutboundJob claimed = transactionTemplate.execute(status -> claimJob(jobId));
        if (claimed == null) {
            return;
        }

        LOGGER.info(
                "Attempt started jobId={} issueId={} idempotencyKey={} attemptCount={}",
                claimed.jobId(),
                claimed.issueId(),
                claimed.idempotencyKey(),
                claimed.attemptCount() + 1
        );

        try {
            SimulatedHttpResponse response = externalEscalationClient.notifyEscalation(
                    claimed.idempotencyKey(),
                    claimed.issueId()
            );
            transactionTemplate.executeWithoutResult(status -> recordOutcome(
                    claimed.jobId(),
                    response.httpStatus(),
                    response.timeoutOrNetworkFailure(),
                    response.errorMessage(),
                    response.retryAfterSeconds()
            ));
        } catch (OutboundTimeoutException exception) {
            transactionTemplate.executeWithoutResult(status -> recordOutcome(
                    claimed.jobId(),
                    null,
                    true,
                    OutboundConstants.SIMULATED_TIMEOUT,
                    null
            ));
        }
    }

    private ClaimedOutboundJob claimJob(Long jobId) {
        OutboundJob job = outboundJobRepository.findById(jobId).orElse(null);
        if (job == null || !CLAIMABLE_STATUSES.contains(job.getStatus())) {
            return null;
        }
        Instant now = retryPolicy.now();
        if (job.getNextAttemptAt().isAfter(now)) {
            return null;
        }
        job.setStatus(OutboundJobStatus.PROCESSING);
        job.setUpdatedAt(now);
        outboundJobRepository.save(job);
        return new ClaimedOutboundJob(
                job.getId(),
                job.getIssue().getId(),
                job.getIdempotencyKey(),
                job.getAttemptCount()
        );
    }

    private void reclaimStaleJobs(Instant now) {
        Instant staleBefore = now.minusSeconds(properties.getStaleProcessingTimeoutSeconds());
        List<OutboundJob> staleJobs = outboundJobRepository.findByStatusAndUpdatedAtLessThan(
                OutboundJobStatus.PROCESSING,
                staleBefore
        );
        for (OutboundJob job : staleJobs) {
            job.setStatus(job.getAttemptCount() == 0 ? OutboundJobStatus.PENDING : OutboundJobStatus.RETRY_SCHEDULED);
            job.setNextAttemptAt(now);
            job.setUpdatedAt(now);
            outboundJobRepository.save(job);
            LOGGER.info(
                    "Reclaimed stale processing job jobId={} issueId={} idempotencyKey={} attemptCount={}",
                    job.getId(),
                    job.getIssue().getId(),
                    job.getIdempotencyKey(),
                    job.getAttemptCount()
            );
        }
    }

    private void recordOutcome(
            Long jobId,
            Integer httpStatus,
            boolean networkOrTimeoutFailure,
            String errorMessage,
            Integer retryAfterSeconds
    ) {
        OutboundJob job = outboundJobRepository.findById(jobId).orElseThrow();
        Issue issue = issueRepository.findById(job.getIssue().getId()).orElseThrow();
        issue.getHistory().size();

        Instant now = retryPolicy.now();
        int attemptCount = job.getAttemptCount() + 1;
        job.setAttemptCount(attemptCount);
        job.setLastAttemptAt(now);
        job.setLastHttpStatus(httpStatus);
        job.setLastError(truncate(errorMessage));
        job.setUpdatedAt(now);

        if (retryClassifier.isSuccess(httpStatus)) {
            job.setStatus(OutboundJobStatus.SUCCEEDED);
            job.setCompletedAt(now);
            job.setLastError(null);
            addHistory(
                    issue,
                    HistoryEventType.ESCALATION_NOTIFICATION_SUCCEEDED,
                    null,
                    String.valueOf(attemptCount),
                    OutboundConstants.HISTORY_SUCCEEDED.formatted(attemptCount)
            );
            outboundJobRepository.save(job);
            issueRepository.save(issue);
            LOGGER.info(
                    "Job succeeded jobId={} issueId={} idempotencyKey={} attemptCount={}",
                    job.getId(),
                    issue.getId(),
                    job.getIdempotencyKey(),
                    attemptCount
            );
            return;
        }

        String failureReason = failureReason(httpStatus, networkOrTimeoutFailure);
        addHistory(
                issue,
                HistoryEventType.ESCALATION_NOTIFICATION_ATTEMPT_FAILED,
                String.valueOf(attemptCount),
                failureReason,
                OutboundConstants.HISTORY_ATTEMPT_FAILED.formatted(attemptCount, failureReason)
        );

        boolean retryable = retryClassifier.isRetryable(httpStatus, networkOrTimeoutFailure);
        boolean attemptsRemain = retryPolicy.hasAttemptsRemaining(attemptCount);

        if (!retryable || !attemptsRemain) {
            job.setStatus(OutboundJobStatus.FAILED);
            job.setCompletedAt(now);
            addHistory(
                    issue,
                    HistoryEventType.ESCALATION_NOTIFICATION_FAILED,
                    null,
                    String.valueOf(attemptCount),
                    OutboundConstants.HISTORY_FAILED.formatted(attemptCount)
            );
            outboundJobRepository.save(job);
            issueRepository.save(issue);
            if (!retryable) {
                LOGGER.info(
                        "Non-retryable failure jobId={} issueId={} idempotencyKey={} attemptCount={}",
                        job.getId(),
                        issue.getId(),
                        job.getIdempotencyKey(),
                        attemptCount
                );
            } else {
                LOGGER.info(
                        "Retry attempts exhausted jobId={} issueId={} idempotencyKey={} attemptCount={}",
                        job.getId(),
                        issue.getId(),
                        job.getIdempotencyKey(),
                        attemptCount
                );
            }
            return;
        }

        Integer retryAfterToHonor = Integer.valueOf(RetryClassificationConstants.HTTP_429).equals(httpStatus)
                ? retryAfterSeconds
                : null;
        Instant nextAttemptAt = retryPolicy.nextAttemptAt(now, attemptCount, retryAfterToHonor);
        job.setStatus(OutboundJobStatus.RETRY_SCHEDULED);
        job.setNextAttemptAt(nextAttemptAt);
        addHistory(
                issue,
                HistoryEventType.ESCALATION_NOTIFICATION_RETRY_SCHEDULED,
                null,
                nextAttemptAt.toString(),
                OutboundConstants.HISTORY_RETRY_SCHEDULED
        );
        outboundJobRepository.save(job);
        issueRepository.save(issue);
        LOGGER.info(
                "Retryable failure jobId={} issueId={} idempotencyKey={} attemptCount={}",
                job.getId(),
                issue.getId(),
                job.getIdempotencyKey(),
                attemptCount
        );
        LOGGER.info(
                "Retry scheduled jobId={} issueId={} idempotencyKey={} attemptCount={}",
                job.getId(),
                issue.getId(),
                job.getIdempotencyKey(),
                attemptCount
        );
    }

    private void addHistory(
            Issue issue,
            HistoryEventType eventType,
            String oldValue,
            String newValue,
            String description
    ) {
        IssueHistory history = new IssueHistory();
        history.setEventType(eventType);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setDescription(description);
        history.setCreatedAt(retryPolicy.now());
        issue.addHistory(history);
    }

    private static String failureReason(Integer httpStatus, boolean networkOrTimeoutFailure) {
        if (networkOrTimeoutFailure || httpStatus == null) {
            return OutboundConstants.FAILURE_TIMEOUT;
        }
        return OutboundConstants.FAILURE_HTTP.formatted(httpStatus);
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= OutboundConstants.LAST_ERROR_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, OutboundConstants.LAST_ERROR_MAX_LENGTH);
    }

    private record ClaimedOutboundJob(Long jobId, Long issueId, String idempotencyKey, int attemptCount) {
    }
}
