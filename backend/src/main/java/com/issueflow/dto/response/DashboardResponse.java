package com.issueflow.dto.response;

public record DashboardResponse(
        long open,
        long critical,
        long inProgress,
        long resolved
) {
}
