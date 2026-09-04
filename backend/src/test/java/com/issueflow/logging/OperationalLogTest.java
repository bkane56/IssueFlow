package com.issueflow.logging;

import com.issueflow.constants.LoggingConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalLogTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationalLogTest.class);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void formatUsesStableKeyValuePairsAndOmitsNulls() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(LoggingConstants.JOB_ID, 21L);
        fields.put(LoggingConstants.HTTP_STATUS, null);
        fields.put(LoggingConstants.OUTCOME, LoggingConstants.OUTCOME_SUCCESS);

        assertThat(OperationalLog.format(LoggingConstants.EVENT_OUTBOUND_JOB_SUCCEEDED, fields))
                .isEqualTo("event=outbound.job.succeeded jobId=21 outcome=SUCCESS");
    }

    @Test
    void infoLogRestoresExistingMdcAfterEmit() {
        MDC.put(LoggingConstants.REQUEST_ID, "req-1");

        OperationalLog.event(LoggingConstants.EVENT_OUTBOUND_JOB_CREATED)
                .put(LoggingConstants.JOB_ID, 21L)
                .put(LoggingConstants.ISSUE_ID, 10L)
                .info(LOGGER);

        assertThat(MDC.get(LoggingConstants.REQUEST_ID)).isEqualTo("req-1");
        assertThat(MDC.get(LoggingConstants.EVENT)).isNull();
        assertThat(MDC.get(LoggingConstants.JOB_ID)).isNull();
    }
}
