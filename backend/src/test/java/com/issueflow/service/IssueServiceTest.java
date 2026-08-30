package com.issueflow.service;

import com.issueflow.constants.ErrorConstants;
import com.issueflow.dto.request.AssignIssueRequest;
import com.issueflow.dto.request.ChangeStatusRequest;
import com.issueflow.dto.request.CreateIssueRequest;
import com.issueflow.dto.request.UpdateIssueRequest;
import com.issueflow.dto.response.IssueResponse;
import com.issueflow.dto.response.PriorityChangeResponse;
import com.issueflow.entity.Category;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import com.issueflow.entity.User;
import com.issueflow.exception.InvalidStateTransitionException;
import com.issueflow.exception.ResourceNotFoundException;
import com.issueflow.mapper.IssueHistoryMapper;
import com.issueflow.mapper.IssueMapper;
import com.issueflow.mapper.TriageMapper;
import com.issueflow.mapper.UserMapper;
import com.issueflow.repository.IssueHistoryRepository;
import com.issueflow.repository.IssueRepository;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueHistoryRepository issueHistoryRepository;

    @Mock
    private UserService userService;

    private IssueService issueService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        TriageService triageService = new TriageService(clock);
        UserMapper userMapper = new UserMapper();
        IssueMapper issueMapper = new IssueMapper(userMapper, new TriageMapper());
        issueService = new IssueService(
                issueRepository,
                issueHistoryRepository,
                userService,
                triageService,
                issueMapper,
                new IssueHistoryMapper(),
                clock
        );
    }

    @Test
    void createRunsTriageAndWritesCreatedHistory() {
        ArgumentCaptor<Issue> savedIssue = ArgumentCaptor.forClass(Issue.class);
        when(issueRepository.save(savedIssue.capture())).thenAnswer(invocation -> {
            Issue issue = invocation.getArgument(0);
            issue.setId(10L);
            return issue;
        });

        IssueResponse response = issueService.create(new CreateIssueRequest(
                "Checkout API returning 500 responses",
                "Payment confirmation fails during peak traffic.",
                Category.BACKEND,
                Severity.CRITICAL,
                null,
                true,
                true,
                120
        ));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.priority()).isEqualTo(Priority.P1);
        assertThat(response.priorityScore()).isGreaterThanOrEqualTo(90);
        assertThat(response.status()).isEqualTo(IssueStatus.NEW);
        assertThat(response.triage().factors()).isNotEmpty();
        assertThat(savedIssue.getValue().getHistory())
                .extracting(history -> history.getEventType())
                .containsExactly(HistoryEventType.ISSUE_CREATED);
    }

    @Test
    void updateRecalculatesPriorityAndRecordsHistory() {
        Issue issue = existingIssue(IssueStatus.NEW, Priority.P4, 0);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssueResponse response = issueService.update(10L, new UpdateIssueRequest(
                "Checkout API returning 500 responses",
                "Payment confirmation fails during peak traffic.",
                Category.BACKEND,
                Severity.CRITICAL,
                null,
                true,
                true,
                150
        ));

        assertThat(response.priority()).isEqualTo(Priority.P1);
        assertThat(issue.getHistory()).extracting(history -> history.getEventType())
                .contains(HistoryEventType.ISSUE_UPDATED, HistoryEventType.PRIORITY_CHANGED);
    }

    @ParameterizedTest
    @CsvSource({
            "NEW, TRIAGED",
            "TRIAGED, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED"
    })
    void changeStatusAcceptsValidTransitions(IssueStatus current, IssueStatus next) {
        Issue issue = existingIssue(current, Priority.P4, 0);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssueResponse response = issueService.changeStatus(10L, new ChangeStatusRequest(next));

        assertThat(response.status()).isEqualTo(next);
        assertThat(issue.getHistory()).extracting(history -> history.getEventType())
                .containsExactly(HistoryEventType.STATUS_CHANGED);
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Issue issue = existingIssue(IssueStatus.CLOSED, Priority.P4, 0);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> issueService.changeStatus(10L, new ChangeStatusRequest(IssueStatus.IN_PROGRESS)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void assignWritesAssigneeHistory() {
        User user = new User("Alex Chen", "alex.chen@issueflow.local", true);
        user.setId(3L);
        Issue issue = existingIssue(IssueStatus.NEW, Priority.P4, 0);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
        when(userService.getUser(3L)).thenReturn(user);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssueResponse response = issueService.assign(10L, new AssignIssueRequest(3L));

        assertThat(response.assignedUser().name()).isEqualTo("Alex Chen");
        assertThat(issue.getHistory()).extracting(history -> history.getEventType())
                .containsExactly(HistoryEventType.ASSIGNEE_CHANGED);
    }

    @Test
    void unassignClearsAssigneeAndWritesHistory() {
        User user = new User("Alex Chen", "alex.chen@issueflow.local", true);
        user.setId(3L);
        Issue issue = existingIssue(IssueStatus.NEW, Priority.P4, 0);
        issue.setAssignedUser(user);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssueResponse response = issueService.assign(10L, new AssignIssueRequest(null));

        assertThat(response.assignedUser()).isNull();
        assertThat(issue.getAssignedUser()).isNull();
        assertThat(issue.getHistory()).extracting(history -> history.getEventType())
                .containsExactly(HistoryEventType.ASSIGNEE_CHANGED);
        assertThat(issue.getHistory().get(0).getOldValue()).isEqualTo("Alex Chen");
        assertThat(issue.getHistory().get(0).getNewValue()).isEqualTo("Unassigned");
    }

    @Test
    void findByIdThrowsWhenIssueIsMissing() {
        assertMissingIssue(() -> issueService.findById(1042L));
    }

    @Test
    void updateThrowsWhenIssueIsMissing() {
        assertMissingIssue(() -> issueService.update(1042L, new UpdateIssueRequest(
                "Admin export is incomplete",
                "CSV export omits rows when the date range is large.",
                Category.FRONTEND,
                Severity.LOW,
                null,
                false,
                false,
                0
        )));
    }

    @Test
    void deleteThrowsWhenIssueIsMissing() {
        assertMissingIssue(() -> issueService.delete(1042L));
        verify(issueRepository, never()).delete(any(Issue.class));
    }

    @Test
    void changeStatusThrowsWhenIssueIsMissing() {
        assertMissingIssue(() -> issueService.changeStatus(1042L, new ChangeStatusRequest(IssueStatus.TRIAGED)));
    }

    @Test
    void assignThrowsWhenIssueIsMissing() {
        assertMissingIssue(() -> issueService.assign(1042L, new AssignIssueRequest(3L)));
    }

    @Test
    void recalculateTriageThrowsWhenIssueIsMissing() {
        assertMissingIssue(() -> issueService.recalculateTriage(1042L));
    }

    @Test
    void findHistoryThrowsWhenIssueIsMissing() {
        assertMissingIssue(() -> issueService.findHistory(1042L));
    }

    @Test
    void recalculateWritesHistoryOnlyWhenPriorityChanges() {
        Issue changing = existingIssue(IssueStatus.NEW, Priority.P4, 0);
        changing.setSeverity(Severity.CRITICAL);
        changing.setProductionImpact(true);
        changing.setCustomerFacing(true);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(changing));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PriorityChangeResponse changed = issueService.recalculateTriage(10L);

        assertThat(changed.changed()).isTrue();
        assertThat(changed.previousPriority()).isEqualTo(Priority.P4);
        assertThat(changed.currentPriority()).isEqualTo(Priority.P1);
        assertThat(changing.getHistory()).extracting(history -> history.getEventType())
                .contains(HistoryEventType.PRIORITY_CHANGED, HistoryEventType.TRIAGE_RECALCULATED);

        Issue unchanged = existingIssue(IssueStatus.NEW, Priority.P4, 0);
        when(issueRepository.findById(11L)).thenReturn(Optional.of(unchanged));

        PriorityChangeResponse same = issueService.recalculateTriage(11L);

        assertThat(same.changed()).isFalse();
        assertThat(unchanged.getHistory()).isEmpty();
    }

    private void assertMissingIssue(ThrowableAssert.ThrowingCallable action) {
        when(issueRepository.findById(1042L)).thenReturn(Optional.empty());
        assertThatThrownBy(action)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorConstants.ISSUE_NOT_FOUND.formatted(1042L));
    }

    private Issue existingIssue(IssueStatus status, Priority priority, int score) {
        Issue issue = new Issue();
        issue.setId(10L);
        issue.setTitle("Admin export is incomplete");
        issue.setDescription("CSV export omits rows when the date range is large.");
        issue.setCategory(Category.FRONTEND);
        issue.setSeverity(Severity.LOW);
        issue.setPriority(priority);
        issue.setPriorityScore(score);
        issue.setStatus(status);
        issue.setCustomerFacing(false);
        issue.setProductionImpact(false);
        issue.setAffectedUsers(0);
        issue.setCreatedAt(NOW);
        issue.setUpdatedAt(NOW);
        return issue;
    }
}
