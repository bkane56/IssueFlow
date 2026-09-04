package com.issueflow.controller;

import com.issueflow.constants.ApiConstants;
import com.issueflow.dto.request.AssignIssueRequest;
import com.issueflow.dto.request.ChangeStatusRequest;
import com.issueflow.dto.request.CreateIssueRequest;
import com.issueflow.dto.request.UpdateIssueRequest;
import com.issueflow.dto.response.IssueHistoryResponse;
import com.issueflow.dto.response.IssueResponse;
import com.issueflow.dto.response.OutboundJobResponse;
import com.issueflow.dto.response.PriorityChangeResponse;
import com.issueflow.entity.Category;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import com.issueflow.service.IssueService;
import com.issueflow.service.OutboundNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_BASE_PATH + ApiConstants.ISSUES_PATH)
public class IssueController {

    private final IssueService issueService;
    private final OutboundNotificationService outboundNotificationService;

    public IssueController(IssueService issueService, OutboundNotificationService outboundNotificationService) {
        this.issueService = issueService;
        this.outboundNotificationService = outboundNotificationService;
    }

    @GetMapping
    public List<IssueResponse> list(
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) String search
    ) {
        return issueService.findAll(status, priority, severity, category, assignedUserId, search);
    }

    @GetMapping("/{id}")
    public IssueResponse get(@PathVariable Long id) {
        return issueService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse create(@Valid @RequestBody CreateIssueRequest request) {
        return issueService.create(request);
    }

    @PutMapping("/{id}")
    public IssueResponse update(@PathVariable Long id, @Valid @RequestBody UpdateIssueRequest request) {
        return issueService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        issueService.delete(id);
    }

    @PatchMapping(ApiConstants.ISSUE_STATUS_PATH)
    public IssueResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return issueService.changeStatus(id, request);
    }

    @PatchMapping(ApiConstants.ISSUE_ASSIGN_PATH)
    public IssueResponse assign(@PathVariable Long id, @RequestBody AssignIssueRequest request) {
        return issueService.assign(id, request);
    }

    @PostMapping(ApiConstants.ISSUE_TRIAGE_PATH)
    public PriorityChangeResponse recalculateTriage(@PathVariable Long id) {
        return issueService.recalculateTriage(id);
    }

    @GetMapping(ApiConstants.ISSUE_HISTORY_PATH)
    public List<IssueHistoryResponse> history(@PathVariable Long id) {
        return issueService.findHistory(id);
    }

    @PostMapping(ApiConstants.ISSUE_ESCALATION_NOTIFICATION_PATH)
    public OutboundJobResponse enqueueEscalation(@PathVariable Long id) {
        return outboundNotificationService.enqueueEscalation(id);
    }

    @GetMapping(ApiConstants.ISSUE_OUTBOUND_JOBS_PATH)
    public List<OutboundJobResponse> outboundJobs(@PathVariable Long id) {
        return outboundNotificationService.findByIssueId(id);
    }
}
