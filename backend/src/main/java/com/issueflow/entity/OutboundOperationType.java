package com.issueflow.entity;

import com.issueflow.constants.OutboundConstants;

public enum OutboundOperationType {
    ESCALATION_NOTIFICATION;

    public String idempotencyKey(Long issueId) {
        return name() + OutboundConstants.IDEMPOTENCY_KEY_SEPARATOR + issueId;
    }
}
