package com.issueflow.repository;

import com.issueflow.entity.Category;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueHistory;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import com.issueflow.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IssuePersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueHistoryRepository issueHistoryRepository;

    @Test
    void savesAndReloadsUserAssignmentAndHistory() {
        User user = userRepository.save(new User("Alex Chen", "alex.chen@issueflow.local", true));
        Instant createdAt = Instant.parse("2026-08-30T12:00:00Z");

        Issue issue = new Issue();
        issue.setTitle("Database connection pool saturation during peak traffic");
        issue.setDescription("The primary order database exhausted its connection pool.");
        issue.setCategory(Category.DATABASE);
        issue.setSeverity(Severity.CRITICAL);
        issue.setPriority(Priority.P1);
        issue.setPriorityScore(110);
        issue.setStatus(IssueStatus.NEW);
        issue.setAssignedUser(user);
        issue.setCustomerFacing(true);
        issue.setProductionImpact(true);
        issue.setAffectedUsers(200);
        issue.setCreatedAt(createdAt);
        issue.setUpdatedAt(createdAt);

        IssueHistory history = new IssueHistory();
        history.setEventType(HistoryEventType.ISSUE_CREATED);
        history.setNewValue(IssueStatus.NEW.name());
        history.setDescription("Issue created");
        history.setCreatedAt(createdAt);
        issue.addHistory(history);

        Issue saved = issueRepository.saveAndFlush(issue);
        Long issueId = saved.getId();
        Long userId = user.getId();
        Long historyId = saved.getHistory().get(0).getId();

        assertThat(issueId).isNotNull();
        assertThat(historyId).isNotNull();
        assertThat(saved.getHistory().get(0).getIssue().getId()).isEqualTo(issueId);

        Issue loaded = issueRepository.findById(issueId).orElseThrow();
        assertThat(loaded.getTitle()).isEqualTo(issue.getTitle());
        assertThat(loaded.getAssignedUser()).isNotNull();
        assertThat(loaded.getAssignedUser().getId()).isEqualTo(userId);
        assertThat(loaded.getAssignedUser().getEmail()).isEqualTo("alex.chen@issueflow.local");

        assertThat(loaded.getHistory()).hasSize(1);
        assertThat(loaded.getHistory().get(0).getId()).isEqualTo(historyId);
        assertThat(loaded.getHistory().get(0).getEventType()).isEqualTo(HistoryEventType.ISSUE_CREATED);
        assertThat(loaded.getHistory().get(0).getIssue().getId()).isEqualTo(issueId);

        assertThat(issueHistoryRepository.findByIssueIdOrderByCreatedAtAsc(issueId))
                .extracting(IssueHistory::getId)
                .containsExactly(historyId);
        assertThat(userRepository.findById(userId)).isPresent();
    }
}
