package com.issueflow.service;

import com.issueflow.config.OutboundProperties;
import com.issueflow.constants.OutboundConstants;
import com.issueflow.constants.RetryClassificationConstants;
import com.issueflow.exception.OutboundTimeoutException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process stand-in for a remote escalation API.
 * Attempt counters and accepted keys are process-local and reset on restart.
 * A real HTTP client must translate transport-library exceptions into
 * {@link com.issueflow.exception.OutboundTransportException} or
 * {@link com.issueflow.exception.OutboundTimeoutException} before they leave this adapter.
 */
@Component
public class SimulatedExternalEscalationClient implements ExternalEscalationClient {

    private final OutboundProperties properties;
    private final ConcurrentHashMap<String, Integer> attemptCounts = new ConcurrentHashMap<>();
    private final Set<String> acceptedKeys = ConcurrentHashMap.newKeySet();

    public SimulatedExternalEscalationClient(OutboundProperties properties) {
        this.properties = properties;
    }

    @Override
    public SimulatedHttpResponse notifyEscalation(String idempotencyKey, Long issueId) {
        if (acceptedKeys.contains(idempotencyKey)) {
            return SimulatedHttpResponse.success(false);
        }

        int attempt = attemptCounts.merge(idempotencyKey, 1, Integer::sum);
        SimulatedHttpResponse response = responseForAttempt(attempt);
        if (response.httpStatus() != null
                && response.httpStatus() >= RetryClassificationConstants.HTTP_SUCCESS_MIN
                && response.httpStatus() < RetryClassificationConstants.HTTP_SUCCESS_MAX_EXCLUSIVE) {
            acceptedKeys.add(idempotencyKey);
            return SimulatedHttpResponse.success(true);
        }
        return response;
    }

    int acceptedCount() {
        return acceptedKeys.size();
    }

    private SimulatedHttpResponse responseForAttempt(int attempt) {
        return switch (properties.getSimulation().getMode()) {
            case ALWAYS_SUCCEED -> SimulatedHttpResponse.success(true);
            case FAIL_ONCE_THEN_SUCCEED -> attempt == 1
                    ? SimulatedHttpResponse.http(RetryClassificationConstants.HTTP_503, OutboundConstants.SIMULATED_HTTP_503)
                    : SimulatedHttpResponse.success(true);
            case FAIL_TWICE_THEN_SUCCEED -> attempt <= 2
                    ? SimulatedHttpResponse.http(RetryClassificationConstants.HTTP_503, OutboundConstants.SIMULATED_HTTP_503)
                    : SimulatedHttpResponse.success(true);
            case ALWAYS_503 -> SimulatedHttpResponse.http(
                    RetryClassificationConstants.HTTP_503,
                    OutboundConstants.SIMULATED_HTTP_503
            );
            case ALWAYS_400 -> SimulatedHttpResponse.http(
                    RetryClassificationConstants.HTTP_400,
                    OutboundConstants.SIMULATED_HTTP_400
            );
            case ALWAYS_TIMEOUT -> throw new OutboundTimeoutException(OutboundConstants.SIMULATED_TIMEOUT);
            case RATE_LIMIT_THEN_SUCCEED -> attempt == 1
                    ? SimulatedHttpResponse.rateLimited(
                            OutboundConstants.SIMULATED_RETRY_AFTER_SECONDS,
                            OutboundConstants.SIMULATED_HTTP_429
                    )
                    : SimulatedHttpResponse.success(true);
        };
    }
}
