package com.issueflow.exception;

public class OutboundTimeoutException extends OutboundTransportException {

    public OutboundTimeoutException(String message) {
        super(message);
    }

    public OutboundTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
