package com.issueflow.service;

import com.issueflow.entity.Priority;

import java.util.List;

public record TriageResult(int score, Priority priority, List<TriageFactor> factors) {
}
