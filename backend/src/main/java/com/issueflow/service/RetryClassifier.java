package com.issueflow.service;

import com.issueflow.constants.RetryClassificationConstants;
import org.springframework.stereotype.Service;

@Service
public class RetryClassifier {

    public boolean isSuccess(Integer httpStatus) {
        return httpStatus != null
                && httpStatus >= RetryClassificationConstants.HTTP_SUCCESS_MIN
                && httpStatus < RetryClassificationConstants.HTTP_SUCCESS_MAX_EXCLUSIVE;
    }

    public boolean isRetryable(Integer httpStatus, boolean networkOrTimeoutFailure) {
        if (isSuccess(httpStatus)) {
            return false;
        }
        if (networkOrTimeoutFailure) {
            return true;
        }
        if (httpStatus == null) {
            return true;
        }
        return RetryClassificationConstants.RETRYABLE_HTTP_STATUSES.contains(httpStatus);
    }
}
