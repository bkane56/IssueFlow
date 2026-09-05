package com.issueflow.service;

import com.issueflow.config.OutboundProperties;
import com.issueflow.constants.LoggingConstants;
import com.issueflow.constants.OutboundConstants;
import com.issueflow.constants.RetryClassificationConstants;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueHistory;
import com.issueflow.entity.OutboundJob;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.exception.OutboundTransportException;
import com.issueflow.logging.OperationalLog;
import com.issueflow.repository.IssueRepository;
import com.issueflow.repository.OutboundJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        OperationalLog.event(LoggingConstants.EVENT_OUTBOUND_WORKER_CLAIMED)
                .put(LoggingConstants.CLAIMED_COUNT, dueJobIds.size())
                .info(LOGGER);
        for (Long jobId : dueJobIds) {
            processJob(jobId);
        }
    }

    private void processJob(Long jobId) {
        ClaimedOutboundJob claimed = transactionTemplate.execute(status -> claimJob(jobId));
        if (claimed == null) {
            return;
        }

        OperationalLog.event(LoggingConstants.EVENT_OUTBOUND_JOB_ATTEMPT_STARTED)
                .put(LoggingConstants.JOB_ID, claimed.jobId())
                .put(LoggingConstants.ISSUE_ID, claimed.issueId())
                .put(LoggingConstants.IDEMPOTENCY_KEY, claimed.idempotencyKey())
                .put(LoggingConstants.ATTEMPT_COUNT, claimed.attemptCount() + 1)
                .info(LOGGER);

        long startedAt = System.nanoTime();
        Integer httpStatus = null;
        boolean timeoutOrNetworkFailure = false;
        String errorMessage = null;
        Integer retryAfterSeconds = null;
        try {
            SimulatedHttpResponse response = externalEscalationClient.notifyEscalation(
                    claimed.idempotencyKey(),
                    claimed.issueId()
            );
            httpStatus = response.httpStatus();
            timeoutOrNetworkFailure = response.timeoutOrNetworkFailure();
            errorMessage = response.errorMessage();
            retryAfterSeconds = response.retryAfterSeconds();
        } catch (OutboundTransportException exception) {
            timeoutOrNetworkFailure = true;
            errorMessage = exception.getMessage();
        }
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        Integer completedHttpStatus = httpStatus;
        boolean completedTimeout = timeoutOrNetworkFailure;
        String completedError = errorMessage;
        Integer completedRetryAfter = retryAfterSeconds;
        transactionTemplate.executeWithoutResult(status -> recordOutcome(
                claimed.jobId(),
                completedHttpStatus,
                completedTimeout,
                completedError,
                completedRetryAfter,
                durationMs
        ));
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
            OperationalLog.event(LoggingConstants.EVENT_OUTBOUND_JOB_RECLAIMED)
                    .put(LoggingConstants.JOB_ID, job.getId())
                    .put(LoggingConstants.ISSUE_ID, job.getIssue().getId())
                    .put(LoggingConstants.IDEMPOTENCY_KEY, job.getIdempotencyKey())
                    .put(LoggingConstants.ATTEMPT_COUNT, job.getAttemptCount())
                    .put(LoggingConstants.JOB_STATUS, job.getStatus())
                    .info(LOGGER);
        }
    }

    private void recordOutcome(
            Long jobId,
            Integer httpStatus,
            boolean networkOrTimeoutFailure,
            String errorMessage,
            Integer retryAfterSeconds,
            long durationMs
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
            OperationalLog.event(LoggingConstants.EVENT_OUTBOUND_JOB_SUCCEEDED)
                    .put(LoggingConstants.JOB_ID, job.getId())
                    .put(LoggingConstants.ISSUE_ID, issue.getId())
                    .put(LoggingConstants.IDEMPOTENCY_KEY, job.getIdempotencyKey())
                    .put(LoggingConstants.ATTEMPT_COUNT, attemptCount)
                    .put(LoggingConstants.HTTP_STATUS, httpStatus)
                    .put(LoggingConstants.DURATION_MS, durationMs)
                    .put(LoggingConstants.JOB_STATUS, job.getStatus())
                    .put(LoggingConstants.OUTCOME, LoggingConstants.OUTCOME_SUCCESS)
                    .info(LOGGER);
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
                OperationalLog.event(LoggingConstants.EVENT_OUTBOUND_JOB_NON_RETRYABLE_FAILURE)
                        .put(LoggingConstants.JOB_ID, job.getId())
                        .put(LoggingConstants.ISSUE_ID, issue.getId())
                        .put(LoggingConstants.IDEMPOTENCY_KEY, job.getIdempotencyKey())
                        .put(LoggingConstants.ATTEMPT_COUNT, attemptCount)
                        .put(LoggingConstants.HTTP_STATUS, httpStatus)
                        .put(LoggingConstants.DURATION_MS, durationMs)
                        .put(LoggingConstants.FAILURE_CLASS, failureClass(httpStatus, networkOrTimeoutFailure))
                        .put(LoggingConstants.RETRYABLE, false)
                        .put(LoggingConstants.JOB_STATUS, job.getStatus())
                        .put(LoggingConstants.OUTCOME, LoggingConstants.OUTCOME_FAILED_NON_RETRYABLE)
                        .info(LOGGER);
            } else {
                OperationalLog.event(LoggingConstants.EVENT_OUTBOUND_JOB_ATTEMPTS_EXHAUSTED)
                        .put(LoggingConstants.JOB_ID, job.getId())
                        .put(LoggingConstants.ISSUE_ID, issue.getId())
                        .put(LoggingConstants.IDEMPOTENCY_KEY, job.getIdempotencyKey())
                        .put(LoggingConstants.ATTEMPT_COUNT, attemptCount)
                        .put(LoggingConstants.HTTP_STATUS, httpStatus)
                        .put(LoggingConstants.DURATION_MS, durationMs)
                        .put(LoggingConstants.FAILURE_CLASS, failureClass(httpStatus, networkOrTimeoutFailure))
                        .put(LoggingConstants.RETRYABLE, true)
                        .put(LoggingConstants.JOB_STATUS, job.getStatus())
                        .put(LoggingConstants.OUTCOME, LoggingConstants.OUTCOME_FAILED_EXHAUSTED)
                        .info(LOGGER);
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
        String classifiedFailure = failureClass(httpStatus, networkOrTimeoutFailure);
        long nextAttemptDelaySeconds = Duration.between(now, nextAttemptAt).getSeconds();
        OperationalLog.event(LoggingConstants.EVENT_OUTBOUND_JOB_RETRYABLE_FAILURE)
                .put(LoggingConstants.JOB_ID, job.getId())
                .put(LoggingConstants.ISSUE_ID, issue.getId())
                .put(LoggingConstants.IDEMPOTENCY_KEY, job.getIdempotencyKey())
                .put(LoggingConstants.ATTEMPT_COUNT, attemptCount)
                .put(LoggingConstants.HTTP_STATUS, httpStatus)
                .put(LoggingConstants.DURATION_MS, durationMs)
                .put(LoggingConstants.FAILURE_CLASS, classifiedFailure)
                .put(LoggingConstants.RETRYABLE, true)
                .put(LoggingConstants.OUTCOME, LoggingConstants.OUTCOME_RETRY_SCHEDULED)
                .info(LOGGER);
        OperationalLog.event(LoggingConstants.EVENT_OUTBOUND_JOB_RETRY_SCHEDULED)
                .put(LoggingConstants.JOB_ID, job.getId())
                .put(LoggingConstants.ISSUE_ID, issue.getId())
                .put(LoggingConstants.IDEMPOTENCY_KEY, job.getIdempotencyKey())
                .put(LoggingConstants.ATTEMPT_COUNT, attemptCount)
                .put(LoggingConstants.HTTP_STATUS, httpStatus)
                .put(LoggingConstants.FAILURE_CLASS, classifiedFailure)
                .put(LoggingConstants.NEXT_ATTEMPT_DELAY_SECONDS, nextAttemptDelaySeconds)
                .put(LoggingConstants.JOB_STATUS, job.getStatus())
                .put(LoggingConstants.OUTCOME, LoggingConstants.OUTCOME_RETRY_SCHEDULED)
                .info(LOGGER);
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

    private static String failureClass(Integer httpStatus, boolean networkOrTimeoutFailure) {
        if (networkOrTimeoutFailure || httpStatus == null) {
            return LoggingConstants.FAILURE_CLASS_TIMEOUT;
        }
        if (httpStatus == RetryClassificationConstants.HTTP_429) {
            return LoggingConstants.FAILURE_CLASS_HTTP_429;
        }
        if (httpStatus >= 500) {
            return LoggingConstants.FAILURE_CLASS_HTTP_5XX;
        }
        if (httpStatus >= 400) {
            return LoggingConstants.FAILURE_CLASS_HTTP_4XX;
        }
        return LoggingConstants.FAILURE_CLASS_HTTP_OTHER;
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
