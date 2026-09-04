package com.issueflow.service;

import com.issueflow.constants.RetryClassificationConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryClassifierTest {

    private RetryClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new RetryClassifier();
    }

    @Test
    void treats2xxAsSuccessAndNotRetryable() {
        assertThat(classifier.isSuccess(200)).isTrue();
        assertThat(classifier.isRetryable(200, false)).isFalse();
        assertThat(classifier.isSuccess(201)).isTrue();
        assertThat(classifier.isRetryable(204, false)).isFalse();
    }

    @Test
    void treatsTimeoutAndNetworkFailuresAsRetryable() {
        assertThat(classifier.isRetryable(null, true)).isTrue();
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_500, true)).isTrue();
        assertThat(classifier.isSuccess(null)).isFalse();
    }

    @Test
    void treatsMissingStatusWithoutNetworkFailureAsRetryable() {
        assertThat(classifier.isRetryable(null, false)).isTrue();
    }

    @Test
    void treats408AsRetryable() {
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_408, false)).isTrue();
        assertThat(classifier.isSuccess(RetryClassificationConstants.HTTP_408)).isFalse();
    }

    @Test
    void treats429AsRetryable() {
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_429, false)).isTrue();
    }

    @Test
    void treats5xxGatewayFailuresAsRetryable() {
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_500, false)).isTrue();
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_502, false)).isTrue();
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_503, false)).isTrue();
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_504, false)).isTrue();
    }

    @Test
    void treatsClientErrorsAsNonRetryable() {
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_400, false)).isFalse();
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_401, false)).isFalse();
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_403, false)).isFalse();
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_404, false)).isFalse();
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_422, false)).isFalse();
    }

    @Test
    void treats409AsNonRetryable() {
        assertThat(classifier.isRetryable(RetryClassificationConstants.HTTP_409, false)).isFalse();
    }
}
