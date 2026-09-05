package com.issueflow.exception;

public class OutboundTransportException extends RuntimeException {

    public OutboundTransportException(String message) {
        super(message);
    }

    public OutboundTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
