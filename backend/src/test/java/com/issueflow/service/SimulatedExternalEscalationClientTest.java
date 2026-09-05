package com.issueflow.service;

import com.issueflow.config.OutboundProperties;
import com.issueflow.constants.OutboundConstants;
import com.issueflow.constants.RetryClassificationConstants;
import com.issueflow.entity.OutboundSimulationMode;
import com.issueflow.exception.OutboundTimeoutException;
import com.issueflow.exception.OutboundTransportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedExternalEscalationClientTest {

    private OutboundProperties properties;
    private SimulatedExternalEscalationClient client;

    @BeforeEach
    void setUp() {
        properties = new OutboundProperties();
        client = new SimulatedExternalEscalationClient(properties);
    }

    @Test
    void alwaysSucceedRecordsOneSideEffectForRepeatedKeys() {
        properties.getSimulation().setMode(OutboundSimulationMode.ALWAYS_SUCCEED);

        SimulatedHttpResponse first = client.notifyEscalation("ESCALATION_NOTIFICATION:1", 1L);
        SimulatedHttpResponse second = client.notifyEscalation("ESCALATION_NOTIFICATION:1", 1L);

        assertThat(first.httpStatus()).isEqualTo(200);
        assertThat(first.sideEffectRecorded()).isTrue();
        assertThat(second.httpStatus()).isEqualTo(200);
        assertThat(second.sideEffectRecorded()).isFalse();
        assertThat(client.acceptedCount()).isEqualTo(1);
    }

    @Test
    void failOnceThenSucceedsOnSecondAttempt() {
        properties.getSimulation().setMode(OutboundSimulationMode.FAIL_ONCE_THEN_SUCCEED);

        SimulatedHttpResponse first = client.notifyEscalation("key-a", 8L);
        SimulatedHttpResponse second = client.notifyEscalation("key-a", 8L);

        assertThat(first.httpStatus()).isEqualTo(RetryClassificationConstants.HTTP_503);
        assertThat(first.sideEffectRecorded()).isFalse();
        assertThat(second.httpStatus()).isEqualTo(200);
        assertThat(second.sideEffectRecorded()).isTrue();
        assertThat(client.acceptedCount()).isEqualTo(1);
    }

    @Test
    void failTwiceThenSucceedsOnThirdAttempt() {
        properties.getSimulation().setMode(OutboundSimulationMode.FAIL_TWICE_THEN_SUCCEED);

        assertThat(client.notifyEscalation("key-b", 8L).httpStatus()).isEqualTo(RetryClassificationConstants.HTTP_503);
        assertThat(client.notifyEscalation("key-b", 8L).httpStatus()).isEqualTo(RetryClassificationConstants.HTTP_503);
        SimulatedHttpResponse third = client.notifyEscalation("key-b", 8L);
        assertThat(third.httpStatus()).isEqualTo(200);
        assertThat(third.sideEffectRecorded()).isTrue();
    }

    @Test
    void always400NeverRecordsSideEffect() {
        properties.getSimulation().setMode(OutboundSimulationMode.ALWAYS_400);

        SimulatedHttpResponse response = client.notifyEscalation("key-c", 8L);
        assertThat(response.httpStatus()).isEqualTo(RetryClassificationConstants.HTTP_400);
        assertThat(response.errorMessage()).isEqualTo(OutboundConstants.SIMULATED_HTTP_400);
        assertThat(client.acceptedCount()).isZero();
    }

    @Test
    void alwaysTimeoutThrowsImmediately() {
        properties.getSimulation().setMode(OutboundSimulationMode.ALWAYS_TIMEOUT);

        assertThatThrownBy(() -> client.notifyEscalation("key-d", 8L))
                .isInstanceOf(OutboundTimeoutException.class)
                .isInstanceOf(OutboundTransportException.class)
                .hasMessage(OutboundConstants.SIMULATED_TIMEOUT);
        assertThat(client.acceptedCount()).isZero();
    }

    @Test
    void rateLimitHonorsRetryAfterThenSucceeds() {
        properties.getSimulation().setMode(OutboundSimulationMode.RATE_LIMIT_THEN_SUCCEED);

        SimulatedHttpResponse first = client.notifyEscalation("key-e", 8L);
        SimulatedHttpResponse second = client.notifyEscalation("key-e", 8L);

        assertThat(first.httpStatus()).isEqualTo(RetryClassificationConstants.HTTP_429);
        assertThat(first.retryAfterSeconds()).isEqualTo(OutboundConstants.SIMULATED_RETRY_AFTER_SECONDS);
        assertThat(second.httpStatus()).isEqualTo(200);
        assertThat(client.acceptedCount()).isEqualTo(1);
    }
}
