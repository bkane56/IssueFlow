package com.issueflow.service;

import com.issueflow.config.OutboundProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class RetryPolicy {

    private final OutboundProperties properties;
    private final Clock clock;

    public RetryPolicy(OutboundProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean hasAttemptsRemaining(int attemptCount) {
        return attemptCount < properties.getMaxAttempts();
    }

    public Duration delayForAttempt(int attemptCountAfterFailure) {
        List<Integer> backoffSeconds = properties.getBackoffSeconds();
        if (backoffSeconds == null || backoffSeconds.isEmpty() || attemptCountAfterFailure < 1) {
            return Duration.ZERO;
        }
        int index = Math.min(attemptCountAfterFailure, backoffSeconds.size()) - 1;
        return Duration.ofSeconds(backoffSeconds.get(index));
    }

    public Instant nextAttemptAt(int attemptCountAfterFailure, Integer retryAfterSeconds) {
        return nextAttemptAt(now(), attemptCountAfterFailure, retryAfterSeconds);
    }

    public Instant nextAttemptAt(Instant now, int attemptCountAfterFailure, Integer retryAfterSeconds) {
        if (retryAfterSeconds != null) {
            int cappedSeconds = Math.max(0, Math.min(retryAfterSeconds, properties.getRetryAfterMaxSeconds()));
            return now.plusSeconds(cappedSeconds);
        }
        return now.plus(delayForAttempt(attemptCountAfterFailure));
    }

    public Instant now() {
        return Instant.now(clock);
    }
}
