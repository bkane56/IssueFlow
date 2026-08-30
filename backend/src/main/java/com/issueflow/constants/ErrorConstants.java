package com.issueflow.constants;

public final class ErrorConstants {

    public static final String ISSUE_NOT_FOUND = "Issue %s was not found";
    public static final String USER_NOT_FOUND = "User %s was not found";
    public static final String EMAIL_IN_USE = "An assignee with that email already exists";
    public static final String INVALID_STATUS_TRANSITION = "Cannot change status from %s to %s";
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String INVALID_REQUEST_BODY = "Request body is invalid or contains an unsupported value";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred";

    private ErrorConstants() {
    }
}
