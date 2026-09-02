package com.issueflow.service;

import com.issueflow.constants.TriageConstants;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TriageServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");

    private TriageService triageService;

    @BeforeEach
    void setUp() {
        triageService = new TriageService(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void calculatesP1ForStackedCriticalFactors() {
        Issue issue = baseIssue();
        issue.setProductionImpact(true);
        issue.setSeverity(Severity.CRITICAL);
        issue.setCustomerFacing(true);

        TriageResult result = triageService.calculate(issue);

        assertThat(result.score()).isEqualTo(110);
        assertThat(result.priority()).isEqualTo(Priority.P1);
        assertThat(result.factors()).extracting(TriageFactor::name)
                .containsExactly(
                        TriageConstants.FACTOR_PRODUCTION_IMPACT,
                        TriageConstants.FACTOR_CRITICAL_SEVERITY,
                        TriageConstants.FACTOR_CUSTOMER_FACING
                );
        assertThat(result.factors()).extracting(TriageFactor::score).containsExactly(50, 40, 20);
    }

    @Test
    void calculatesP2ForHighSeverityProductionIssue() {
        Issue issue = baseIssue();
        issue.setProductionImpact(true);
        issue.setSeverity(Severity.HIGH);

        TriageResult result = triageService.calculate(issue);

        assertThat(result.score()).isEqualTo(75);
        assertThat(result.priority()).isEqualTo(Priority.P2);
    }

    @Test
    void calculatesP3ForMediumCustomerFacingIssue() {
        Issue issue = baseIssue();
        issue.setSeverity(Severity.MEDIUM);
        issue.setCustomerFacing(true);

        TriageResult result = triageService.calculate(issue);

        assertThat(result.score()).isEqualTo(30);
        assertThat(result.priority()).isEqualTo(Priority.P3);
    }

    @Test
    void calculatesP4ForLowImpactIssue() {
        Issue issue = baseIssue();
        issue.setSeverity(Severity.LOW);

        TriageResult result = triageService.calculate(issue);

        assertThat(result.score()).isZero();
        assertThat(result.priority()).isEqualTo(Priority.P4);
        assertThat(result.factors()).isEmpty();
    }

    @Test
    void scoresProductionImpact() {
        Issue issue = baseIssue();
        issue.setProductionImpact(true);

        assertThat(triageService.calculate(issue).score()).isEqualTo(TriageConstants.PRODUCTION_IMPACT_SCORE);
    }

    @Test
    void scoresCustomerFacing() {
        Issue issue = baseIssue();
        issue.setCustomerFacing(true);

        assertThat(triageService.calculate(issue).score()).isEqualTo(TriageConstants.CUSTOMER_FACING_SCORE);
    }

    @Test
    void scoresSeverityLevels() {
        Issue critical = baseIssue();
        critical.setSeverity(Severity.CRITICAL);
        Issue high = baseIssue();
        high.setSeverity(Severity.HIGH);
        Issue medium = baseIssue();
        medium.setSeverity(Severity.MEDIUM);

        assertThat(triageService.calculate(critical).score()).isEqualTo(TriageConstants.CRITICAL_SEVERITY_SCORE);
        assertThat(triageService.calculate(high).score()).isEqualTo(TriageConstants.HIGH_SEVERITY_SCORE);
        assertThat(triageService.calculate(medium).score()).isEqualTo(TriageConstants.MEDIUM_SEVERITY_SCORE);
    }

    @Test
    void scoresAffectedUserThresholds() {
        Issue high = baseIssue();
        high.setAffectedUsers(100);
        Issue medium = baseIssue();
        medium.setAffectedUsers(25);
        Issue below = baseIssue();
        below.setAffectedUsers(24);

        Issue justBelowHigh = baseIssue();
        justBelowHigh.setAffectedUsers(99);

        assertThat(triageService.calculate(high).score()).isEqualTo(TriageConstants.AFFECTED_USERS_HIGH_SCORE);
        assertThat(triageService.calculate(medium).score()).isEqualTo(TriageConstants.AFFECTED_USERS_MEDIUM_SCORE);
        assertThat(triageService.calculate(justBelowHigh).score()).isEqualTo(TriageConstants.AFFECTED_USERS_MEDIUM_SCORE);
        assertThat(triageService.calculate(below).score()).isZero();
    }

    @Test
    void scoresAgeWhenUnresolvedAndOlderThan24Hours() {
        Issue issue = baseIssue();
        issue.setCreatedAt(NOW.minus(Duration.ofHours(24)));
        issue.setStatus(IssueStatus.NEW);

        TriageResult result = triageService.calculate(issue);

        assertThat(result.score()).isEqualTo(TriageConstants.AGE_UNRESOLVED_SCORE);
        assertThat(result.factors()).extracting(TriageFactor::name)
                .containsExactly(TriageConstants.FACTOR_AGE_UNRESOLVED);
    }

    @Test
    void doesNotScoreAgeWhenJustUnder24Hours() {
        Issue issue = baseIssue();
        issue.setCreatedAt(NOW.minus(Duration.ofHours(23)));
        issue.setStatus(IssueStatus.NEW);

        assertThat(triageService.calculate(issue).score()).isZero();
    }

    @Test
    void doesNotScoreAgeWhenResolved() {
        Issue issue = baseIssue();
        issue.setCreatedAt(NOW.minus(Duration.ofHours(48)));
        issue.setStatus(IssueStatus.RESOLVED);

        assertThat(triageService.calculate(issue).score()).isZero();
    }

    @Test
    void doesNotScoreAgeWhenClosed() {
        Issue issue = baseIssue();
        issue.setCreatedAt(NOW.minus(Duration.ofHours(48)));
        issue.setStatus(IssueStatus.CLOSED);

        assertThat(triageService.calculate(issue).score()).isZero();
    }

    @Test
    void usesPriorityThresholdBoundaries() {
        assertThat(triageService.toPriority(29)).isEqualTo(Priority.P4);
        assertThat(triageService.toPriority(30)).isEqualTo(Priority.P3);
        assertThat(triageService.toPriority(59)).isEqualTo(Priority.P3);
        assertThat(triageService.toPriority(60)).isEqualTo(Priority.P2);
        assertThat(triageService.toPriority(89)).isEqualTo(Priority.P2);
        assertThat(triageService.toPriority(90)).isEqualTo(Priority.P1);
    }

    private Issue baseIssue() {
        Issue issue = new Issue();
        issue.setSeverity(Severity.LOW);
        issue.setStatus(IssueStatus.NEW);
        issue.setCustomerFacing(false);
        issue.setProductionImpact(false);
        issue.setAffectedUsers(0);
        issue.setCreatedAt(NOW);
        return issue;
    }
}
