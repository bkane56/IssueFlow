package com.issueflow.constants;

public final class ApiConstants {

    public static final String API_BASE_PATH = "/api";
    public static final String ISSUES_PATH = "/issues";
    public static final String USERS_PATH = "/users";
    public static final String DASHBOARD_PATH = "/dashboard";
    public static final String ISSUE_STATUS_PATH = "/{id}/status";
    public static final String ISSUE_ASSIGN_PATH = "/{id}/assign";
    public static final String ISSUE_TRIAGE_PATH = "/{id}/triage";
    public static final String ISSUE_HISTORY_PATH = "/{id}/history";
    public static final String ISSUE_ESCALATION_NOTIFICATION_PATH = "/{id}/escalation-notification";
    public static final String ISSUE_OUTBOUND_JOBS_PATH = "/{id}/outbound-jobs";
    public static final String OUTBOUND_JOBS_PATH = "/outbound-jobs";
    public static final String CORS_LOCAL_ORIGIN = "http://localhost:3000";

    private ApiConstants() {
    }
}
