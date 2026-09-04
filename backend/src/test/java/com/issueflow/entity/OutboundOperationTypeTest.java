package com.issueflow.entity;

import com.issueflow.constants.OutboundConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundOperationTypeTest {

    @Test
    void buildsStableIdempotencyKeyFromIssueId() {
        assertThat(OutboundOperationType.ESCALATION_NOTIFICATION.idempotencyKey(42L))
                .isEqualTo("ESCALATION_NOTIFICATION" + OutboundConstants.IDEMPOTENCY_KEY_SEPARATOR + "42");
    }
}
