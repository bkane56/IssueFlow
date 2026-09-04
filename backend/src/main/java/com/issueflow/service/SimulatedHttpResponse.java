package com.issueflow.service;

public record SimulatedHttpResponse(
        Integer httpStatus,
        Integer retryAfterSeconds,
        String errorMessage,
        boolean timeoutOrNetworkFailure,
        boolean sideEffectRecorded
) {

    public static SimulatedHttpResponse success(boolean sideEffectRecorded) {
        return new SimulatedHttpResponse(200, null, null, false, sideEffectRecorded);
    }

    public static SimulatedHttpResponse http(int httpStatus, String errorMessage) {
        return new SimulatedHttpResponse(httpStatus, null, errorMessage, false, false);
    }

    public static SimulatedHttpResponse rateLimited(int retryAfterSeconds, String errorMessage) {
        return new SimulatedHttpResponse(429, retryAfterSeconds, errorMessage, false, false);
    }
}
