package com.issueflow.controller;

import com.issueflow.config.TimeConfig;
import com.issueflow.constants.ErrorConstants;
import com.issueflow.dto.response.OutboundJobResponse;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.entity.OutboundOperationType;
import com.issueflow.exception.ResourceNotFoundException;
import com.issueflow.service.OutboundNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutboundJobController.class)
@Import(TimeConfig.class)
class OutboundJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OutboundNotificationService outboundNotificationService;

    @Test
    void returnsOutboundJobById() throws Exception {
        when(outboundNotificationService.findById(21L)).thenReturn(sampleJob());

        mockMvc.perform(get("/api/outbound-jobs/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(21))
                .andExpect(jsonPath("$.idempotencyKey").value("ESCALATION_NOTIFICATION:10"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void returnsNotFoundForMissingJob() throws Exception {
        when(outboundNotificationService.findById(99L))
                .thenThrow(new ResourceNotFoundException(ErrorConstants.OUTBOUND_JOB_NOT_FOUND.formatted(99L)));

        mockMvc.perform(get("/api/outbound-jobs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ErrorConstants.OUTBOUND_JOB_NOT_FOUND.formatted(99L)))
                .andExpect(jsonPath("$.path").value("/api/outbound-jobs/99"));
    }

    private OutboundJobResponse sampleJob() {
        Instant now = Instant.parse("2026-09-04T12:00:00Z");
        return new OutboundJobResponse(
                21L,
                OutboundOperationType.ESCALATION_NOTIFICATION,
                "ESCALATION_NOTIFICATION:10",
                OutboundJobStatus.PENDING,
                0,
                now,
                null,
                null,
                now,
                now,
                null
        );
    }
}
