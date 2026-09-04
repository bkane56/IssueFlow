package com.issueflow.controller;

import com.issueflow.constants.ApiConstants;
import com.issueflow.dto.response.OutboundJobResponse;
import com.issueflow.service.OutboundNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_BASE_PATH + ApiConstants.OUTBOUND_JOBS_PATH)
public class OutboundJobController {

    private final OutboundNotificationService outboundNotificationService;

    public OutboundJobController(OutboundNotificationService outboundNotificationService) {
        this.outboundNotificationService = outboundNotificationService;
    }

    @GetMapping("/{jobId}")
    public OutboundJobResponse get(@PathVariable Long jobId) {
        return outboundNotificationService.findById(jobId);
    }
}
