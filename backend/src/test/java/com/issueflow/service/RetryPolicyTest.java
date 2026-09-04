package com.issueflow.service;

import com.issueflow.config.OutboundProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {

    private static final Instant NOW = Instant.parse("2026-09-04T12:00:00Z");

    private OutboundProperties properties;
    private RetryPolicy retryPolicy;

    @BeforeEach
    void setUp() {
        properties = new OutboundProperties();
        properties.setMaxAttempts(5);
        properties.setBackoffSeconds(List.of(5, 15, 45, 120));
        properties.setRetryAfterMaxSeconds(120);
        retryPolicy = new RetryPolicy(properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void usesImmediateDelayForInvalidAttemptCount() {
        assertThat(retryPolicy.delayForAttempt(0)).isEqualTo(Duration.ZERO);
        assertThat(retryPolicy.delayForAttempt(-1)).isEqualTo(Duration.ZERO);
    }

    @Test
    void usesConfiguredBackoffSequence() {
        assertThat(retryPolicy.delayForAttempt(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(retryPolicy.delayForAttempt(2)).isEqualTo(Duration.ofSeconds(15));
        assertThat(retryPolicy.delayForAttempt(3)).isEqualTo(Duration.ofSeconds(45));
        assertThat(retryPolicy.delayForAttempt(4)).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void reusesFinalBackoffWhenAttemptExceedsConfiguredDelays() {
        assertThat(retryPolicy.delayForAttempt(5)).isEqualTo(Duration.ofSeconds(120));
        assertThat(retryPolicy.delayForAttempt(8)).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void allowsRetriesUntilMaxAttemptsAreReached() {
        assertThat(retryPolicy.hasAttemptsRemaining(0)).isTrue();
        assertThat(retryPolicy.hasAttemptsRemaining(4)).isTrue();
        assertThat(retryPolicy.hasAttemptsRemaining(5)).isFalse();
    }

    @Test
    void schedulesNextAttemptFromBackoffWhenRetryAfterIsAbsent() {
        assertThat(retryPolicy.nextAttemptAt(1, null)).isEqualTo(NOW.plusSeconds(5));
        assertThat(retryPolicy.nextAttemptAt(2, null)).isEqualTo(NOW.plusSeconds(15));
        assertThat(retryPolicy.nextAttemptAt(3, null)).isEqualTo(NOW.plusSeconds(45));
        assertThat(retryPolicy.nextAttemptAt(4, null)).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void honorsRetryAfterWhenProvided() {
        assertThat(retryPolicy.nextAttemptAt(1, 30)).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void capsRetryAfterAtConfiguredMaximum() {
        assertThat(retryPolicy.nextAttemptAt(1, 3600)).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void treatsNegativeRetryAfterAsZeroDelay() {
        assertThat(retryPolicy.nextAttemptAt(1, -10)).isEqualTo(NOW);
    }

    @Test
    void usesInjectedClockForNow() {
        assertThat(retryPolicy.now()).isEqualTo(NOW);
    }
}
