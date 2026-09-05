package com.issueflow.service;

public interface ExternalEscalationClient {

    /**
     * Calls the external escalation endpoint.
     * Transient communication failures must be thrown as
     * {@link com.issueflow.exception.OutboundTransportException}
     * or {@link com.issueflow.exception.OutboundTimeoutException}.
     * HTTP-library exception types must not escape this adapter.
     */
    SimulatedHttpResponse notifyEscalation(String idempotencyKey, Long issueId);
}
