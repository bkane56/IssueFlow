package com.issueflow.service;

import com.issueflow.constants.TriageConstants;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TriageService {

    private final Clock clock;

    public TriageService(Clock clock) {
        this.clock = clock;
    }

    public TriageResult calculate(Issue issue) {
        List<TriageFactor> factors = new ArrayList<>();

        if (issue.isProductionImpact()) {
            factors.add(new TriageFactor(
                    TriageConstants.FACTOR_PRODUCTION_IMPACT,
                    TriageConstants.PRODUCTION_IMPACT_SCORE
            ));
        }

        addSeverityFactor(issue.getSeverity(), factors);

        if (issue.isCustomerFacing()) {
            factors.add(new TriageFactor(
                    TriageConstants.FACTOR_CUSTOMER_FACING,
                    TriageConstants.CUSTOMER_FACING_SCORE
            ));
        }

        addAffectedUsersFactor(issue.getAffectedUsers(), factors);
        addAgeFactor(issue, factors);

        int score = factors.stream().mapToInt(TriageFactor::score).sum();
        return new TriageResult(score, toPriority(score), List.copyOf(factors));
    }

    private void addSeverityFactor(Severity severity, List<TriageFactor> factors) {
        if (severity == Severity.CRITICAL) {
            factors.add(new TriageFactor(
                    TriageConstants.FACTOR_CRITICAL_SEVERITY,
                    TriageConstants.CRITICAL_SEVERITY_SCORE
            ));
        } else if (severity == Severity.HIGH) {
            factors.add(new TriageFactor(
                    TriageConstants.FACTOR_HIGH_SEVERITY,
                    TriageConstants.HIGH_SEVERITY_SCORE
            ));
        } else if (severity == Severity.MEDIUM) {
            factors.add(new TriageFactor(
                    TriageConstants.FACTOR_MEDIUM_SEVERITY,
                    TriageConstants.MEDIUM_SEVERITY_SCORE
            ));
        }
    }

    private void addAffectedUsersFactor(int affectedUsers, List<TriageFactor> factors) {
        if (affectedUsers >= TriageConstants.AFFECTED_USERS_HIGH_THRESHOLD) {
            factors.add(new TriageFactor(
                    TriageConstants.FACTOR_AFFECTED_USERS_HIGH,
                    TriageConstants.AFFECTED_USERS_HIGH_SCORE
            ));
        } else if (affectedUsers >= TriageConstants.AFFECTED_USERS_MEDIUM_THRESHOLD) {
            factors.add(new TriageFactor(
                    TriageConstants.FACTOR_AFFECTED_USERS_MEDIUM,
                    TriageConstants.AFFECTED_USERS_MEDIUM_SCORE
            ));
        }
    }

    private void addAgeFactor(Issue issue, List<TriageFactor> factors) {
        if (issue.getCreatedAt() == null || !isUnresolved(issue.getStatus())) {
            return;
        }
        Duration age = Duration.between(issue.getCreatedAt(), Instant.now(clock));
        if (age.toHours() >= TriageConstants.AGE_UNRESOLVED_HOURS) {
            factors.add(new TriageFactor(
                    TriageConstants.FACTOR_AGE_UNRESOLVED,
                    TriageConstants.AGE_UNRESOLVED_SCORE
            ));
        }
    }

    private boolean isUnresolved(IssueStatus status) {
        return status != IssueStatus.RESOLVED && status != IssueStatus.CLOSED;
    }

    Priority toPriority(int score) {
        if (score >= TriageConstants.P1_THRESHOLD) {
            return Priority.P1;
        }
        if (score >= TriageConstants.P2_THRESHOLD) {
            return Priority.P2;
        }
        if (score >= TriageConstants.P3_THRESHOLD) {
            return Priority.P3;
        }
        return Priority.P4;
    }
}
