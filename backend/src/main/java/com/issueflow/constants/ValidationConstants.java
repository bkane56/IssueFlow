package com.issueflow.constants;

public final class ValidationConstants {

    public static final int TITLE_MAX_LENGTH = 200;
    public static final int DESCRIPTION_MAX_LENGTH = 4000;
    public static final int NAME_MAX_LENGTH = 120;
    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int HISTORY_DESCRIPTION_MAX_LENGTH = 1000;

    public static final String TITLE_REQUIRED = "Title is required";
    public static final String DESCRIPTION_REQUIRED = "Description is required";
    public static final String SEVERITY_REQUIRED = "Severity is required";
    public static final String CATEGORY_REQUIRED = "Category is required";
    public static final String STATUS_REQUIRED = "Status is required";
    public static final String NAME_REQUIRED = "Name is required";
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_INVALID = "Email must be a valid address";
    public static final String AFFECTED_USERS_MIN = "Affected users cannot be negative";

    private ValidationConstants() {
    }
}
