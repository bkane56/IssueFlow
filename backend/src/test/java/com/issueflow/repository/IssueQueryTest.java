package com.issueflow.repository;

import com.issueflow.entity.Category;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import com.issueflow.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IssueQueryTest {

    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");
    private static final EnumSet<IssueStatus> CLOSED_STATUSES = EnumSet.of(
            IssueStatus.RESOLVED,
            IssueStatus.CLOSED
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IssueRepository issueRepository;

    private User alex;

    @BeforeEach
    void setUp() {
        alex = userRepository.save(new User("Alex Chen", "alex.chen@issueflow.local", true));

        persistIssue(
                "Checkout API returning 500 responses",
                "Payment confirmation fails during peak traffic.",
                IssueStatus.NEW,
                Priority.P1,
                Severity.CRITICAL,
                alex,
                NOW
        );
        persistIssue(
                "Admin export is incomplete",
                "CSV export omits rows when the date range is large.",
                IssueStatus.IN_PROGRESS,
                Priority.P2,
                Severity.HIGH,
                null,
                NOW.minusSeconds(60)
        );
        persistIssue(
                "Password reset emails delayed",
                "Reset messages arrived after the token expired.",
                IssueStatus.RESOLVED,
                Priority.P1,
                Severity.CRITICAL,
                alex,
                NOW.minusSeconds(120)
        );
        persistIssue(
                "Stale documentation link on login page",
                "The help article URL returns 404.",
                IssueStatus.CLOSED,
                Priority.P4,
                Severity.LOW,
                null,
                NOW.minusSeconds(180)
        );
    }

    @Test
    void filtersByStatusPriorityAndAssignee() {
        assertThat(titles(IssueStatus.NEW, null, null, null))
                .containsExactly("Checkout API returning 500 responses");
        assertThat(titles(null, Priority.P2, null, null))
                .containsExactly("Admin export is incomplete");
        assertThat(titles(null, null, alex.getId(), null))
                .containsExactly(
                        "Checkout API returning 500 responses",
                        "Password reset emails delayed"
                );
    }

    @Test
    void searchMatchesTitleAndDescriptionIgnoringCase() {
        assertThat(titles(null, null, null, "CHECKOUT"))
                .containsExactly("Checkout API returning 500 responses");
        assertThat(titles(null, null, null, "omits rows"))
                .containsExactly("Admin export is incomplete");
    }

    @Test
    void blankSearchReturnsAllIssues() {
        assertThat(titles(null, null, null, "   ")).hasSize(4);
        assertThat(titles(null, null, null, null)).hasSize(4);
    }

    @Test
    void dashboardCountsExcludeClosedStatusesFromOpenAndCritical() {
        assertThat(issueRepository.countByStatusNotIn(CLOSED_STATUSES)).isEqualTo(2);
        assertThat(issueRepository.countBySeverityAndStatusNotIn(Severity.CRITICAL, CLOSED_STATUSES)).isEqualTo(1);
        assertThat(issueRepository.countByStatus(IssueStatus.IN_PROGRESS)).isEqualTo(1);
        assertThat(issueRepository.countByStatus(IssueStatus.RESOLVED)).isEqualTo(1);
    }

    private List<String> titles(IssueStatus status, Priority priority, Long assignedUserId, String search) {
        return issueRepository.findAll(
                        IssueSpecifications.withFilters(status, priority, null, null, assignedUserId, search),
                        Sort.by(Sort.Direction.DESC, "updatedAt")
                ).stream()
                .map(Issue::getTitle)
                .toList();
    }

    private void persistIssue(
            String title,
            String description,
            IssueStatus status,
            Priority priority,
            Severity severity,
            User assignedUser,
            Instant updatedAt
    ) {
        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setCategory(Category.BACKEND);
        issue.setSeverity(severity);
        issue.setPriority(priority);
        issue.setPriorityScore(0);
        issue.setStatus(status);
        issue.setAssignedUser(assignedUser);
        issue.setCustomerFacing(false);
        issue.setProductionImpact(false);
        issue.setAffectedUsers(0);
        issue.setCreatedAt(updatedAt);
        issue.setUpdatedAt(updatedAt);
        issueRepository.saveAndFlush(issue);
    }
}
