package com.issueflow.mapper;

import com.issueflow.dto.response.TriageFactorResponse;
import com.issueflow.dto.response.TriageResultResponse;
import com.issueflow.service.TriageFactor;
import com.issueflow.service.TriageResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TriageMapper {

    public TriageResultResponse toResponse(TriageResult result) {
        List<TriageFactorResponse> factors = result.factors().stream()
                .map(this::toFactorResponse)
                .toList();
        return new TriageResultResponse(result.score(), result.priority(), factors);
    }

    private TriageFactorResponse toFactorResponse(TriageFactor factor) {
        return new TriageFactorResponse(factor.name(), factor.score());
    }
}
