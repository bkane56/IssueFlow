package com.issueflow.exception;

public class OutboundTimeoutException extends RuntimeException {

    public OutboundTimeoutException(String message) {
        super(message);
    }
}
