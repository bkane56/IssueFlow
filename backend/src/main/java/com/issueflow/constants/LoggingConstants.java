package com.issueflow.constants;

import java.util.List;

public final class LoggingConstants {

    public static final String EVENT = "event";
    public static final String REQUEST_ID = "requestId";
    public static final String OUTCOME = "outcome";
    public static final String DURATION_MS = "durationMs";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String HTTP_ROUTE = "httpRoute";
    public static final String HTTP_STATUS = "httpStatus";
    public static final String EXCEPTION_CLASS = "exceptionClass";
    public static final String JOB_ID = "jobId";
    public static final String ISSUE_ID = "issueId";
    public static final String IDEMPOTENCY_KEY = "idempotencyKey";
    public static final String ATTEMPT_COUNT = "attemptCount";
    public static final String OPERATION_TYPE = "operationType";
    public static final String JOB_STATUS = "jobStatus";
    public static final String FAILURE_CLASS = "failureClass";
    public static final String RETRYABLE = "retryable";
    public static final String NEXT_ATTEMPT_DELAY_SECONDS = "nextAttemptDelaySeconds";
    public static final String CLAIMED_COUNT = "claimedCount";
    public static final String CATEGORY = "category";
    public static final String SEVERITY = "severity";
    public static final String PRIORITY = "priority";
    public static final String CUSTOMER_FACING = "customerFacing";
    public static final String PRODUCTION_IMPACT = "productionImpact";
    public static final String STATUS_FROM = "statusFrom";
    public static final String STATUS_TO = "statusTo";
    public static final String ASSIGNED = "assigned";
    public static final String PRIORITY_CHANGED = "priorityChanged";
    public static final String REASON = "reason";

    public static final String EVENT_HTTP_REQUEST = "http.request";
    public static final String EVENT_HTTP_ERROR = "http.error";
    public static final String EVENT_ISSUE_CREATED = "issue.created";
    public static final String EVENT_ISSUE_STATUS_CHANGED = "issue.status_changed";
    public static final String EVENT_ISSUE_ASSIGNED = "issue.assigned";
    public static final String EVENT_ISSUE_TRIAGE_RECALCULATED = "issue.triage_recalculated";
    public static final String EVENT_ISSUE_DELETED = "issue.deleted";
    public static final String EVENT_OUTBOUND_JOB_CREATED = "outbound.job.created";
    public static final String EVENT_OUTBOUND_JOB_DUPLICATE = "outbound.job.duplicate";
    public static final String EVENT_OUTBOUND_JOB_REJECTED = "outbound.job.rejected";
    public static final String EVENT_OUTBOUND_JOB_ATTEMPT_STARTED = "outbound.job.attempt_started";
    public static final String EVENT_OUTBOUND_JOB_RETRYABLE_FAILURE = "outbound.job.retryable_failure";
    public static final String EVENT_OUTBOUND_JOB_NON_RETRYABLE_FAILURE = "outbound.job.non_retryable_failure";
    public static final String EVENT_OUTBOUND_JOB_RETRY_SCHEDULED = "outbound.job.retry_scheduled";
    public static final String EVENT_OUTBOUND_JOB_SUCCEEDED = "outbound.job.succeeded";
    public static final String EVENT_OUTBOUND_JOB_ATTEMPTS_EXHAUSTED = "outbound.job.attempts_exhausted";
    public static final String EVENT_OUTBOUND_JOB_RECLAIMED = "outbound.job.reclaimed";
    public static final String EVENT_OUTBOUND_WORKER_CLAIMED = "outbound.worker.claimed";

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_CLIENT_ERROR = "CLIENT_ERROR";
    public static final String OUTCOME_SERVER_ERROR = "SERVER_ERROR";
    public static final String OUTCOME_RETRY_SCHEDULED = "RETRY_SCHEDULED";
    public static final String OUTCOME_FAILED_NON_RETRYABLE = "FAILED_NON_RETRYABLE";
    public static final String OUTCOME_FAILED_EXHAUSTED = "FAILED_EXHAUSTED";
    public static final String OUTCOME_REJECTED = "REJECTED";
    public static final String OUTCOME_DUPLICATE = "DUPLICATE";

    public static final String FAILURE_CLASS_TIMEOUT = "TIMEOUT";
    public static final String FAILURE_CLASS_HTTP_429 = "HTTP_429";
    public static final String FAILURE_CLASS_HTTP_5XX = "HTTP_5XX";
    public static final String FAILURE_CLASS_HTTP_4XX = "HTTP_4XX";
    public static final String FAILURE_CLASS_HTTP_OTHER = "HTTP_OTHER";

    public static final String REASON_ISSUE_CLOSED = "ISSUE_CLOSED";

    public static final List<String> HTTP_SKIP_PATH_PREFIXES = List.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/favicon.ico"
    );

    private LoggingConstants() {
    }
}
