package com.issueflow.service;

public interface ExternalEscalationClient {

    SimulatedHttpResponse notifyEscalation(String idempotencyKey, Long issueId);
}
