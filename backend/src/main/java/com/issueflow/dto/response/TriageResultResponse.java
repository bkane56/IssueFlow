package com.issueflow.dto.response;

import com.issueflow.entity.Priority;

import java.util.List;

public record TriageResultResponse(
        int score,
        Priority priority,
        List<TriageFactorResponse> factors
) {
}
