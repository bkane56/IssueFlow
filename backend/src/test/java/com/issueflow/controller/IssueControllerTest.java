package com.issueflow.controller;

import com.issueflow.config.TimeConfig;
import com.issueflow.constants.ErrorConstants;
import com.issueflow.dto.request.AssignIssueRequest;
import com.issueflow.dto.request.ChangeStatusRequest;
import com.issueflow.dto.request.CreateIssueRequest;
import com.issueflow.dto.request.UpdateIssueRequest;
import com.issueflow.dto.response.IssueHistoryResponse;
import com.issueflow.dto.response.IssueResponse;
import com.issueflow.dto.response.OutboundJobResponse;
import com.issueflow.dto.response.PriorityChangeResponse;
import com.issueflow.dto.response.TriageResultResponse;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Category;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.entity.OutboundOperationType;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import com.issueflow.exception.InvalidStateTransitionException;
import com.issueflow.exception.ResourceNotFoundException;
import com.issueflow.service.IssueService;
import com.issueflow.service.OutboundNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IssueController.class)
@Import(TimeConfig.class)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private OutboundNotificationService outboundNotificationService;

    @Test
    void listsIssues() throws Exception {
        when(issueService.findAll(null, null, null, null, null, null)).thenReturn(List.of(sampleIssue()));

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Checkout API returning 500 responses"))
                .andExpect(jsonPath("$[0].priority").value("P1"));
    }

    @Test
    void listsIssuesWithStatusFilter() throws Exception {
        when(issueService.findAll(IssueStatus.NEW, null, null, null, null, null))
                .thenReturn(List.of(sampleIssue()));

        mockMvc.perform(get("/api/issues").param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void returnsIssueById() throws Exception {
        when(issueService.findById(10L)).thenReturn(sampleIssue());

        mockMvc.perform(get("/api/issues/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Checkout API returning 500 responses"));
    }

    @Test
    void createsIssue() throws Exception {
        when(issueService.create(any(CreateIssueRequest.class))).thenReturn(sampleIssue());

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Checkout API returning 500 responses",
                                  "description": "Payment confirmation fails during peak traffic.",
                                  "category": "BACKEND",
                                  "severity": "CRITICAL",
                                  "customerFacing": true,
                                  "productionImpact": true,
                                  "affectedUsers": 120
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Missing title",
                                  "category": "BACKEND",
                                  "severity": "HIGH",
                                  "affectedUsers": -4
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void updatesIssue() throws Exception {
        when(issueService.update(eq(10L), any(UpdateIssueRequest.class))).thenReturn(sampleIssue());

        mockMvc.perform(put("/api/issues/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Checkout API returning 500 responses",
                                  "description": "Payment confirmation fails during peak traffic.",
                                  "category": "BACKEND",
                                  "severity": "CRITICAL",
                                  "customerFacing": true,
                                  "productionImpact": true,
                                  "affectedUsers": 120
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void rejectsInvalidUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/issues/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Missing title",
                                  "category": "BACKEND",
                                  "severity": "HIGH",
                                  "affectedUsers": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deletesIssue() throws Exception {
        mockMvc.perform(delete("/api/issues/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void assignsIssue() throws Exception {
        when(issueService.assign(eq(10L), any(AssignIssueRequest.class))).thenReturn(sampleIssue());

        mockMvc.perform(patch("/api/issues/10/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedUserId": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void recalculatesTriage() throws Exception {
        when(issueService.recalculateTriage(10L)).thenReturn(
                new PriorityChangeResponse(Priority.P4, Priority.P1, true, sampleIssue())
        );

        mockMvc.perform(post("/api/issues/10/triage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.previousPriority").value("P4"))
                .andExpect(jsonPath("$.currentPriority").value("P1"))
                .andExpect(jsonPath("$.issue.id").value(10));
    }

    @Test
    void returnsIssueHistory() throws Exception {
        when(issueService.findHistory(10L)).thenReturn(List.of(
                new IssueHistoryResponse(
                        1L,
                        HistoryEventType.ISSUE_CREATED,
                        null,
                        IssueStatus.NEW.name(),
                        "Issue created",
                        Instant.parse("2026-08-30T12:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/issues/10/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("ISSUE_CREATED"))
                .andExpect(jsonPath("$[0].newValue").value("NEW"));
    }

    @Test
    void rejectsMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(ErrorConstants.INVALID_REQUEST_BODY))
                .andExpect(jsonPath("$.path").value("/api/issues"));
    }

    @Test
    void rejectsInvalidFilterEnum() throws Exception {
        mockMvc.perform(get("/api/issues").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter status"))
                .andExpect(jsonPath("$.path").value("/api/issues"));
    }

    @Test
    void returnsNotFoundForMissingIssue() throws Exception {
        when(issueService.findById(1042L)).thenThrow(new ResourceNotFoundException("Issue 1042 was not found"));

        mockMvc.perform(get("/api/issues/1042"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Issue 1042 was not found"))
                .andExpect(jsonPath("$.path").value("/api/issues/1042"));
    }

    @Test
    void returnsConflictForInvalidStatusTransition() throws Exception {
        when(issueService.changeStatus(eq(10L), any(ChangeStatusRequest.class)))
                .thenThrow(new InvalidStateTransitionException(
                        ErrorConstants.INVALID_STATUS_TRANSITION.formatted(
                                IssueStatus.CLOSED,
                                IssueStatus.IN_PROGRESS
                        )
                ));

        mockMvc.perform(patch("/api/issues/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        ErrorConstants.INVALID_STATUS_TRANSITION.formatted(
                                IssueStatus.CLOSED,
                                IssueStatus.IN_PROGRESS
                        )
                ))
                .andExpect(jsonPath("$.path").value("/api/issues/10/status"));
    }

    @Test
    void queuesEscalationNotification() throws Exception {
        when(outboundNotificationService.enqueueEscalation(10L)).thenReturn(sampleOutboundJob());

        mockMvc.perform(post("/api/issues/10/escalation-notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(21))
                .andExpect(jsonPath("$.idempotencyKey").value("ESCALATION_NOTIFICATION:10"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void returnsExistingJobForDuplicateEscalationTrigger() throws Exception {
        when(outboundNotificationService.enqueueEscalation(10L)).thenReturn(sampleOutboundJob());

        mockMvc.perform(post("/api/issues/10/escalation-notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(21));
        mockMvc.perform(post("/api/issues/10/escalation-notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(21));
    }

    @Test
    void listsOutboundJobsForIssue() throws Exception {
        when(outboundNotificationService.findByIssueId(10L)).thenReturn(List.of(sampleOutboundJob()));

        mockMvc.perform(get("/api/issues/10/outbound-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(21))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void returnsNotFoundWhenEscalatingMissingIssue() throws Exception {
        when(outboundNotificationService.enqueueEscalation(1042L))
                .thenThrow(new ResourceNotFoundException(ErrorConstants.ISSUE_NOT_FOUND.formatted(1042L)));

        mockMvc.perform(post("/api/issues/1042/escalation-notification"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ErrorConstants.ISSUE_NOT_FOUND.formatted(1042L)));
    }

    @Test
    void returnsConflictWhenEscalatingClosedIssue() throws Exception {
        when(outboundNotificationService.enqueueEscalation(10L))
                .thenThrow(new InvalidStateTransitionException(ErrorConstants.ESCALATION_NOT_ALLOWED_FOR_CLOSED_ISSUE));

        mockMvc.perform(post("/api/issues/10/escalation-notification"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(ErrorConstants.ESCALATION_NOT_ALLOWED_FOR_CLOSED_ISSUE));
    }

    private OutboundJobResponse sampleOutboundJob() {
        Instant queuedAt = Instant.parse("2026-09-04T12:00:00Z");
        return new OutboundJobResponse(
                21L,
                OutboundOperationType.ESCALATION_NOTIFICATION,
                "ESCALATION_NOTIFICATION:10",
                OutboundJobStatus.PENDING,
                0,
                queuedAt,
                null,
                null,
                queuedAt,
                queuedAt,
                null
        );
    }

    private IssueResponse sampleIssue() {
        return new IssueResponse(
                10L,
                "Checkout API returning 500 responses",
                "Payment confirmation fails during peak traffic.",
                Category.BACKEND,
                Severity.CRITICAL,
                Priority.P1,
                110,
                IssueStatus.NEW,
                null,
                true,
                true,
                120,
                Instant.parse("2026-08-30T12:00:00Z"),
                Instant.parse("2026-08-30T12:00:00Z"),
                new TriageResultResponse(110, Priority.P1, List.of())
        );
    }
}
