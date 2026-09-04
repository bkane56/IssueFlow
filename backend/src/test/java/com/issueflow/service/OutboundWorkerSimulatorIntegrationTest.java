package com.issueflow.service;

import com.issueflow.config.OutboundProperties;
import com.issueflow.dto.request.CreateIssueRequest;
import com.issueflow.entity.Category;
import com.issueflow.entity.OutboundJob;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.entity.OutboundSimulationMode;
import com.issueflow.entity.Severity;
import com.issueflow.repository.OutboundJobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OutboundWorkerSimulatorIntegrationTest {

    @Autowired
    private IssueService issueService;

    @Autowired
    private OutboundNotificationService outboundNotificationService;

    @Autowired
    private OutboundWorkerService outboundWorkerService;

    @Autowired
    private OutboundJobRepository outboundJobRepository;

    @Autowired
    private SimulatedExternalEscalationClient simulatedExternalEscalationClient;

    @Autowired
    private OutboundProperties outboundProperties;

    private OutboundSimulationMode originalMode;
    private int acceptedBefore;

    @BeforeEach
    void setUp() {
        originalMode = outboundProperties.getSimulation().getMode();
        acceptedBefore = simulatedExternalEscalationClient.acceptedCount();
    }

    @AfterEach
    void tearDown() {
        outboundProperties.getSimulation().setMode(originalMode);
    }

    @Test
    void simulatedRepeatedDeliveryDoesNotCreateDuplicateSideEffects() {
        outboundProperties.getSimulation().setMode(OutboundSimulationMode.ALWAYS_SUCCEED);
        Long issueId = issueService.create(new CreateIssueRequest(
                "Simulator duplicate delivery",
                "Repeated delivery with the same idempotency key must not create two side effects.",
                Category.INTEGRATION,
                Severity.HIGH,
                null,
                true,
                true,
                10
        )).id();
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();
        String idempotencyKey = outboundJobRepository.findById(jobId).orElseThrow().getIdempotencyKey();

        outboundWorkerService.processDueJobs();
        SimulatedHttpResponse replay = simulatedExternalEscalationClient.notifyEscalation(idempotencyKey, issueId);

        OutboundJob job = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(OutboundJobStatus.SUCCEEDED);
        assertThat(replay.httpStatus()).isEqualTo(200);
        assertThat(replay.sideEffectRecorded()).isFalse();
        assertThat(simulatedExternalEscalationClient.acceptedCount()).isEqualTo(acceptedBefore + 1);
    }

    @Test
    void always400FailsWithoutRetry() {
        outboundProperties.getSimulation().setMode(OutboundSimulationMode.ALWAYS_400);
        Long issueId = issueService.create(new CreateIssueRequest(
                "Simulator non-retryable 400",
                "A simulated 400 should fail the outbound job without retry.",
                Category.INTEGRATION,
                Severity.MEDIUM,
                null,
                false,
                false,
                3
        )).id();
        Long jobId = outboundNotificationService.enqueueEscalation(issueId).jobId();

        outboundWorkerService.processDueJobs();

        OutboundJob job = outboundJobRepository.findById(jobId).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(OutboundJobStatus.FAILED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getLastHttpStatus()).isEqualTo(400);
    }
}
