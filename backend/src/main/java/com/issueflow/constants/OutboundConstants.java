package com.issueflow.constants;

import java.util.List;

public final class OutboundConstants {

    public static final int DEFAULT_MAX_ATTEMPTS = 5;
    public static final int DEFAULT_RETRY_AFTER_MAX_SECONDS = 120;
    public static final int DEFAULT_STALE_PROCESSING_TIMEOUT_SECONDS = 30;
    public static final long DEFAULT_WORKER_INTERVAL_MS = 2000L;
    public static final List<Integer> DEFAULT_BACKOFF_SECONDS = List.of(5, 15, 45, 120);

    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 120;
    public static final int LAST_ERROR_MAX_LENGTH = 1000;
    public static final String IDEMPOTENCY_KEY_SEPARATOR = ":";
    public static final String HISTORY_QUEUED = "Escalation notification queued";
    public static final String HISTORY_ATTEMPT_FAILED = "Escalation notification attempt %s failed: %s";
    public static final String HISTORY_RETRY_SCHEDULED = "Escalation notification retry scheduled";
    public static final String HISTORY_SUCCEEDED = "Escalation notification succeeded on attempt %s";
    public static final String HISTORY_FAILED = "Escalation notification permanently failed after %s attempts";
    public static final String FAILURE_TIMEOUT = "timeout";
    public static final String FAILURE_HTTP = "HTTP %s";
    public static final String SIMULATED_TIMEOUT = "Simulated connection timeout";
    public static final String SIMULATED_CONNECTION_REFUSED = "Simulated connection refused";
    public static final String SIMULATED_HTTP_400 = "Simulated HTTP 400";
    public static final String SIMULATED_HTTP_429 = "Simulated HTTP 429";
    public static final String SIMULATED_HTTP_503 = "Simulated HTTP 503";
    public static final int SIMULATED_RETRY_AFTER_SECONDS = 15;

    private OutboundConstants() {
    }
}
