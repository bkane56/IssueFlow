package com.issueflow.constants;

public final class TriageConstants {

    public static final int PRODUCTION_IMPACT_SCORE = 50;
    public static final int CRITICAL_SEVERITY_SCORE = 40;
    public static final int HIGH_SEVERITY_SCORE = 25;
    public static final int MEDIUM_SEVERITY_SCORE = 10;
    public static final int CUSTOMER_FACING_SCORE = 20;
    public static final int AFFECTED_USERS_HIGH_THRESHOLD = 100;
    public static final int AFFECTED_USERS_HIGH_SCORE = 20;
    public static final int AFFECTED_USERS_MEDIUM_THRESHOLD = 25;
    public static final int AFFECTED_USERS_MEDIUM_SCORE = 10;
    public static final int AGE_UNRESOLVED_HOURS = 24;
    public static final int AGE_UNRESOLVED_SCORE = 10;

    public static final int P1_THRESHOLD = 90;
    public static final int P2_THRESHOLD = 60;
    public static final int P3_THRESHOLD = 30;

    public static final String FACTOR_PRODUCTION_IMPACT = "Production impact";
    public static final String FACTOR_CRITICAL_SEVERITY = "Critical severity";
    public static final String FACTOR_HIGH_SEVERITY = "High severity";
    public static final String FACTOR_MEDIUM_SEVERITY = "Medium severity";
    public static final String FACTOR_CUSTOMER_FACING = "Customer facing";
    public static final String FACTOR_AFFECTED_USERS_HIGH = "100 or more affected users";
    public static final String FACTOR_AFFECTED_USERS_MEDIUM = "25 to 99 affected users";
    public static final String FACTOR_AGE_UNRESOLVED = "Older than 24 hours and unresolved";

    private TriageConstants() {
    }
}
