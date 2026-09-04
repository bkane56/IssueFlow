package com.issueflow.entity;

public enum OutboundSimulationMode {
    ALWAYS_SUCCEED,
    FAIL_ONCE_THEN_SUCCEED,
    FAIL_TWICE_THEN_SUCCEED,
    ALWAYS_503,
    ALWAYS_400,
    ALWAYS_TIMEOUT,
    RATE_LIMIT_THEN_SUCCEED
}
