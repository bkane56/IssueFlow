package com.issueflow.constants;

import java.util.Set;

public final class RetryClassificationConstants {

    public static final int HTTP_SUCCESS_MIN = 200;
    public static final int HTTP_SUCCESS_MAX_EXCLUSIVE = 300;

    public static final int HTTP_400 = 400;
    public static final int HTTP_401 = 401;
    public static final int HTTP_403 = 403;
    public static final int HTTP_404 = 404;
    public static final int HTTP_408 = 408;
    public static final int HTTP_409 = 409;
    public static final int HTTP_422 = 422;
    public static final int HTTP_429 = 429;
    public static final int HTTP_500 = 500;
    public static final int HTTP_502 = 502;
    public static final int HTTP_503 = 503;
    public static final int HTTP_504 = 504;

    public static final Set<Integer> RETRYABLE_HTTP_STATUSES = Set.of(
            HTTP_408,
            HTTP_429,
            HTTP_500,
            HTTP_502,
            HTTP_503,
            HTTP_504
    );

    private RetryClassificationConstants() {
    }
}
