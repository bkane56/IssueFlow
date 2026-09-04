package com.issueflow.service;

import com.issueflow.constants.ErrorConstants;
import com.issueflow.constants.LoggingConstants;
import com.issueflow.dto.request.AssignIssueRequest;
import com.issueflow.dto.request.ChangeStatusRequest;
import com.issueflow.dto.request.CreateIssueRequest;
import com.issueflow.dto.request.UpdateIssueRequest;
import com.issueflow.dto.response.IssueHistoryResponse;
import com.issueflow.dto.response.IssueResponse;
import com.issueflow.dto.response.PriorityChangeResponse;
import com.issueflow.entity.Category;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueHistory;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import com.issueflow.entity.User;
import com.issueflow.exception.InvalidStateTransitionException;
import com.issueflow.exception.ResourceNotFoundException;
import com.issueflow.logging.OperationalLog;
import com.issueflow.mapper.IssueHistoryMapper;
import com.issueflow.mapper.IssueMapper;
import com.issueflow.repository.IssueHistoryRepository;
import com.issueflow.repository.IssueRepository;
import com.issueflow.repository.IssueSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class IssueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssueService.class);

    private final IssueRepository issueRepository;
    private final IssueHistoryRepository issueHistoryRepository;
    private final UserService userService;
    private final TriageService triageService;
    private final IssueMapper issueMapper;
    private final IssueHistoryMapper issueHistoryMapper;
    private final Clock clock;

    public IssueService(
            IssueRepository issueRepository,
            IssueHistoryRepository issueHistoryRepository,
            UserService userService,
            TriageService triageService,
            IssueMapper issueMapper,
            IssueHistoryMapper issueHistoryMapper,
            Clock clock
    ) {
        this.issueRepository = issueRepository;
        this.issueHistoryRepository = issueHistoryRepository;
        this.userService = userService;
        this.triageService = triageService;
        this.issueMapper = issueMapper;
        this.issueHistoryMapper = issueHistoryMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> findAll(
            IssueStatus status,
            Priority priority,
            Severity severity,
            Category category,
            Long assignedUserId,
            String search
    ) {
        return issueRepository.findAll(
                IssueSpecifications.withFilters(status, priority, severity, category, assignedUserId, search),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IssueResponse findById(Long id) {
        return toResponse(getIssue(id));
    }

    public IssueResponse create(CreateIssueRequest request) {
        Instant now = Instant.now(clock);
        Issue issue = new Issue();
        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setCategory(request.category());
        issue.setSeverity(request.severity());
        issue.setCustomerFacing(request.customerFacing());
        issue.setProductionImpact(request.productionImpact());
        issue.setAffectedUsers(request.affectedUsers());
        issue.setStatus(IssueStatus.NEW);
        issue.setAssignedUser(resolveUser(request.assignedUserId()));
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);
        applyTriage(issue);
        addHistory(issue, HistoryEventType.ISSUE_CREATED, null, issue.getStatus().name(), "Issue created");
        Issue saved = issueRepository.save(issue);
        OperationalLog.event(LoggingConstants.EVENT_ISSUE_CREATED)
                .put(LoggingConstants.ISSUE_ID, saved.getId())
                .put(LoggingConstants.CATEGORY, saved.getCategory())
                .put(LoggingConstants.SEVERITY, saved.getSeverity())
                .put(LoggingConstants.PRIORITY, saved.getPriority())
                .put(LoggingConstants.CUSTOMER_FACING, saved.isCustomerFacing())
                .put(LoggingConstants.PRODUCTION_IMPACT, saved.isProductionImpact())
                .info(LOGGER);
        return toResponse(saved);
    }

    public IssueResponse update(Long id, UpdateIssueRequest request) {
        Issue issue = getIssue(id);
        Priority previousPriority = issue.getPriority();
        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setCategory(request.category());
        issue.setSeverity(request.severity());
        issue.setCustomerFacing(request.customerFacing());
        issue.setProductionImpact(request.productionImpact());
        issue.setAffectedUsers(request.affectedUsers());
        issue.setAssignedUser(resolveUser(request.assignedUserId()));
        issue.setUpdatedAt(Instant.now(clock));
        applyTriage(issue);
        addHistory(issue, HistoryEventType.ISSUE_UPDATED, null, null, "Issue details updated");
        if (previousPriority != issue.getPriority()) {
            addHistory(
                    issue,
                    HistoryEventType.PRIORITY_CHANGED,
                    previousPriority.name(),
                    issue.getPriority().name(),
                    "Priority updated after issue change"
            );
        }
        return toResponse(issueRepository.save(issue));
    }

    public void delete(Long id) {
        issueRepository.delete(getIssue(id));
        OperationalLog.event(LoggingConstants.EVENT_ISSUE_DELETED)
                .put(LoggingConstants.ISSUE_ID, id)
                .info(LOGGER);
    }

    public IssueResponse changeStatus(Long id, ChangeStatusRequest request) {
        Issue issue = getIssue(id);
        IssueStatus current = issue.getStatus();
        IssueStatus next = request.status();
        if (current == next) {
            return toResponse(issue);
        }
        assertValidTransition(current, next);
        issue.setStatus(next);
        issue.setUpdatedAt(Instant.now(clock));
        addHistory(
                issue,
                HistoryEventType.STATUS_CHANGED,
                current.name(),
                next.name(),
                "Status changed from %s to %s".formatted(current, next)
        );
        Issue saved = issueRepository.save(issue);
        OperationalLog.event(LoggingConstants.EVENT_ISSUE_STATUS_CHANGED)
                .put(LoggingConstants.ISSUE_ID, saved.getId())
                .put(LoggingConstants.STATUS_FROM, current)
                .put(LoggingConstants.STATUS_TO, next)
                .info(LOGGER);
        return toResponse(saved);
    }

    public IssueResponse assign(Long id, AssignIssueRequest request) {
        Issue issue = getIssue(id);
        String previousAssignee = issue.getAssignedUser() == null ? null : issue.getAssignedUser().getName();
        User nextUser = resolveUser(request.assignedUserId());
        Long previousId = issue.getAssignedUser() == null ? null : issue.getAssignedUser().getId();
        Long nextId = nextUser == null ? null : nextUser.getId();
        if (Objects.equals(previousId, nextId)) {
            return toResponse(issue);
        }
        issue.setAssignedUser(nextUser);
        issue.setUpdatedAt(Instant.now(clock));
        String nextAssignee = nextUser == null ? "Unassigned" : nextUser.getName();
        addHistory(
                issue,
                HistoryEventType.ASSIGNEE_CHANGED,
                previousAssignee,
                nextAssignee,
                "Assignee changed"
        );
        Issue saved = issueRepository.save(issue);
        OperationalLog.event(LoggingConstants.EVENT_ISSUE_ASSIGNED)
                .put(LoggingConstants.ISSUE_ID, saved.getId())
                .put(LoggingConstants.ASSIGNED, nextUser != null)
                .info(LOGGER);
        return toResponse(saved);
    }

    public PriorityChangeResponse recalculateTriage(Long id) {
        Issue issue = getIssue(id);
        Priority previousPriority = issue.getPriority();
        int previousScore = issue.getPriorityScore();
        applyTriage(issue);
        boolean priorityChanged = previousPriority != issue.getPriority();
        boolean scoreChanged = previousScore != issue.getPriorityScore();
        if (priorityChanged) {
            addHistory(
                    issue,
                    HistoryEventType.PRIORITY_CHANGED,
                    previousPriority.name(),
                    issue.getPriority().name(),
                    "Priority changed after triage recalculation"
            );
            addHistory(
                    issue,
                    HistoryEventType.TRIAGE_RECALCULATED,
                    previousPriority.name(),
                    issue.getPriority().name(),
                    "Triage recalculated"
            );
        } else if (scoreChanged) {
            addHistory(
                    issue,
                    HistoryEventType.TRIAGE_RECALCULATED,
                    String.valueOf(previousScore),
                    String.valueOf(issue.getPriorityScore()),
                    "Triage score updated without a priority change"
            );
        }
        if (priorityChanged || scoreChanged) {
            issue.setUpdatedAt(Instant.now(clock));
        }
        Issue saved = issueRepository.save(issue);
        if (priorityChanged || scoreChanged) {
            OperationalLog.event(LoggingConstants.EVENT_ISSUE_TRIAGE_RECALCULATED)
                    .put(LoggingConstants.ISSUE_ID, saved.getId())
                    .put(LoggingConstants.PRIORITY, saved.getPriority())
                    .put(LoggingConstants.PRIORITY_CHANGED, priorityChanged)
                    .info(LOGGER);
        }
        return new PriorityChangeResponse(previousPriority, saved.getPriority(), priorityChanged, toResponse(saved));
    }

    @Transactional(readOnly = true)
    public List<IssueHistoryResponse> findHistory(Long id) {
        getIssue(id);
        return issueHistoryRepository.findByIssueIdOrderByCreatedAtAsc(id).stream()
                .map(issueHistoryMapper::toResponse)
                .toList();
    }

    private Issue getIssue(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorConstants.ISSUE_NOT_FOUND.formatted(id)));
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userService.getUser(userId);
    }

    private void applyTriage(Issue issue) {
        TriageResult result = triageService.calculate(issue);
        issue.setPriorityScore(result.score());
        issue.setPriority(result.priority());
    }

    private IssueResponse toResponse(Issue issue) {
        return issueMapper.toResponse(issue, triageService.calculate(issue));
    }

    private void addHistory(
            Issue issue,
            HistoryEventType eventType,
            String oldValue,
            String newValue,
            String description
    ) {
        IssueHistory history = new IssueHistory();
        history.setEventType(eventType);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setDescription(description);
        history.setCreatedAt(Instant.now(clock));
        issue.addHistory(history);
    }

    private void assertValidTransition(IssueStatus current, IssueStatus next) {
        boolean valid = (current == IssueStatus.NEW && next == IssueStatus.TRIAGED)
                || (current == IssueStatus.TRIAGED && next == IssueStatus.IN_PROGRESS)
                || (current == IssueStatus.IN_PROGRESS && next == IssueStatus.RESOLVED)
                || (current == IssueStatus.RESOLVED && next == IssueStatus.CLOSED);
        if (!valid) {
            throw new InvalidStateTransitionException(
                    ErrorConstants.INVALID_STATUS_TRANSITION.formatted(current, next)
            );
        }
    }
}
