package com.issueflow.controller;

import com.issueflow.config.TimeConfig;
import com.issueflow.constants.ErrorConstants;
import com.issueflow.dto.request.ChangeStatusRequest;
import com.issueflow.dto.request.CreateIssueRequest;
import com.issueflow.dto.response.IssueResponse;
import com.issueflow.dto.response.TriageResultResponse;
import com.issueflow.entity.Category;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import com.issueflow.exception.InvalidStateTransitionException;
import com.issueflow.exception.ResourceNotFoundException;
import com.issueflow.service.IssueService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IssueController.class)
@Import(TimeConfig.class)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    @Test
    void listsIssues() throws Exception {
        when(issueService.findAll(null, null, null, null, null, null)).thenReturn(List.of(sampleIssue()));

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Checkout API returning 500 responses"))
                .andExpect(jsonPath("$[0].priority").value("P1"));
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
