package com.issueflow.config;

import com.issueflow.entity.Category;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueHistory;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Severity;
import com.issueflow.entity.User;
import com.issueflow.repository.IssueRepository;
import com.issueflow.repository.UserRepository;
import com.issueflow.service.TriageResult;
import com.issueflow.service.TriageService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class SeedDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final TriageService triageService;
    private final Clock clock;

    public SeedDataLoader(
            UserRepository userRepository,
            IssueRepository issueRepository,
            TriageService triageService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.triageService = triageService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        List<User> users = seedUsers();
        seedIssues(users);
    }

    private List<User> seedUsers() {
        return userRepository.saveAll(List.of(
                new User("Alex Chen", "alex.chen@issueflow.local", true),
                new User("Jordan Patel", "jordan.patel@issueflow.local", true),
                new User("Morgan Rivera", "morgan.rivera@issueflow.local", true),
                new User("Sam Okonkwo", "sam.okonkwo@issueflow.local", true),
                new User("Riley Thompson", "riley.thompson@issueflow.local", true)
        ));
    }

    private void seedIssues(List<User> users) {
        User alex = users.get(0);
        User jordan = users.get(1);
        User morgan = users.get(2);
        User sam = users.get(3);
        User riley = users.get(4);
        Instant now = Instant.now(clock);

        createIssue(
                "Checkout API returning intermittent 500 responses",
                "Customers receive an internal server error during payment confirmation. The failure rate increases during peak checkout volume.",
                Category.BACKEND,
                Severity.CRITICAL,
                IssueStatus.IN_PROGRESS,
                alex,
                true,
                true,
                240,
                now.minus(Duration.ofHours(30))
        );
        createIssue(
                "Customer dashboard fails to load account history",
                "The account history panel stays on a loading state for some retail customers after the latest frontend release.",
                Category.FRONTEND,
                Severity.HIGH,
                IssueStatus.TRIAGED,
                jordan,
                true,
                false,
                80,
                now.minus(Duration.ofHours(10))
        );
        createIssue(
                "Nightly billing reconciliation job exceeded SLA",
                "The reconciliation batch completed 47 minutes after the agreed operations window and delayed finance reports.",
                Category.BACKEND,
                Severity.HIGH,
                IssueStatus.IN_PROGRESS,
                morgan,
                false,
                true,
                12,
                now.minus(Duration.ofHours(40))
        );
        createIssue(
                "Search results occasionally return stale product data",
                "Product search can show prices and inventory from a previous index snapshot for several minutes after a catalog update.",
                Category.INTEGRATION,
                Severity.MEDIUM,
                IssueStatus.TRIAGED,
                riley,
                true,
                false,
                60,
                now.minus(Duration.ofHours(8))
        );
        createIssue(
                "Database connection pool saturation during peak traffic",
                "The primary order database exhausted its connection pool for twelve minutes during the Friday traffic spike.",
                Category.DATABASE,
                Severity.CRITICAL,
                IssueStatus.IN_PROGRESS,
                sam,
                true,
                true,
                310,
                now.minus(Duration.ofHours(26))
        );
        createIssue(
                "Password reset email delivery delayed",
                "Password reset messages are arriving 15 to 40 minutes after the request, causing support tickets from locked-out users.",
                Category.INFRASTRUCTURE,
                Severity.HIGH,
                IssueStatus.NEW,
                alex,
                true,
                false,
                45,
                now.minus(Duration.ofHours(6))
        );
        createIssue(
                "Admin users cannot export incident CSV",
                "The export action on the operations console returns an empty file when more than 500 rows are selected.",
                Category.FRONTEND,
                Severity.MEDIUM,
                IssueStatus.NEW,
                jordan,
                false,
                false,
                8,
                now.minus(Duration.ofHours(3))
        );
        createIssue(
                "TLS certificate renewal failed on edge proxy",
                "Automated certificate renewal logged a validation error. The current certificate expires in five days.",
                Category.SECURITY,
                Severity.CRITICAL,
                IssueStatus.TRIAGED,
                morgan,
                true,
                true,
                500,
                now.minus(Duration.ofHours(18))
        );
        createIssue(
                "Inventory sync worker retries indefinitely",
                "The warehouse inventory connector retries failed SKU updates without a backoff limit and is generating excess API traffic.",
                Category.INTEGRATION,
                Severity.HIGH,
                IssueStatus.IN_PROGRESS,
                sam,
                false,
                true,
                22,
                now.minus(Duration.ofHours(14))
        );
        createIssue(
                "Resolved login banner still appears for some tenants",
                "A previously fixed maintenance banner still renders for two enterprise tenants after they were moved off the old theme.",
                Category.FRONTEND,
                Severity.LOW,
                IssueStatus.RESOLVED,
                riley,
                true,
                false,
                6,
                now.minus(Duration.ofHours(72))
        );
        createIssue(
                "Read replica lag exceeded alerting threshold",
                "Reporting queries against the read replica returned data that was more than four minutes behind the primary.",
                Category.DATABASE,
                Severity.HIGH,
                IssueStatus.RESOLVED,
                alex,
                false,
                true,
                18,
                now.minus(Duration.ofHours(50))
        );
        createIssue(
                "CDN cache purge job skipped image assets",
                "A configuration change caused image assets to be excluded from the nightly cache purge, leaving stale marketing images.",
                Category.INFRASTRUCTURE,
                Severity.MEDIUM,
                IssueStatus.CLOSED,
                jordan,
                true,
                false,
                35,
                now.minus(Duration.ofHours(96))
        );
        createIssue(
                "Webhook signatures rejected by partner gateway",
                "Outbound webhook requests fail partner signature validation after the signing key rotation.",
                Category.INTEGRATION,
                Severity.CRITICAL,
                IssueStatus.NEW,
                morgan,
                false,
                true,
                15,
                now.minus(Duration.ofHours(4))
        );
        createIssue(
                "Support agents cannot attach screenshots over 3 MB",
                "The attachment uploader rejects valid PNG files slightly above 3 MB even though the documented limit is 8 MB.",
                Category.FRONTEND,
                Severity.LOW,
                IssueStatus.TRIAGED,
                riley,
                false,
                false,
                4,
                now.minus(Duration.ofHours(20))
        );
        createIssue(
                "Audit log query times out in operations console",
                "Filtering audit events by a 30-day window exceeds the request timeout and returns an empty error page.",
                Category.DATABASE,
                Severity.MEDIUM,
                IssueStatus.IN_PROGRESS,
                sam,
                false,
                false,
                9,
                now.minus(Duration.ofHours(12))
        );
        createIssue(
                "Feature flag service returned stale toggles",
                "Two production services received outdated feature flag values for 11 minutes after a configuration publish.",
                Category.INFRASTRUCTURE,
                Severity.HIGH,
                IssueStatus.RESOLVED,
                alex,
                false,
                true,
                120,
                now.minus(Duration.ofHours(60))
        );
        createIssue(
                "Session tokens persist after explicit logout",
                "A subset of browsers retain a valid session cookie after logout when the user had multiple tabs open.",
                Category.SECURITY,
                Severity.HIGH,
                IssueStatus.IN_PROGRESS,
                morgan,
                true,
                true,
                55,
                now.minus(Duration.ofHours(9))
        );
        createIssue(
                "Help center search ranking ignores recent articles",
                "Newly published support articles do not appear in the first two pages of results for matching queries.",
                Category.OTHER,
                Severity.LOW,
                IssueStatus.NEW,
                jordan,
                true,
                false,
                27,
                now.minus(Duration.ofHours(5))
        );
        createIssue(
                "Background virus scan queue is backing up",
                "Uploaded files remain in the scanning queue for more than 25 minutes, delaying availability in the document library.",
                Category.INFRASTRUCTURE,
                Severity.MEDIUM,
                IssueStatus.TRIAGED,
                sam,
                false,
                false,
                40,
                now.minus(Duration.ofHours(16))
        );
        createIssue(
                "Closed refund workflow still sends customer emails",
                "A completed refund case continues to send a daily status email because the notification trigger was not cleared.",
                Category.BACKEND,
                Severity.LOW,
                IssueStatus.CLOSED,
                riley,
                true,
                false,
                3,
                now.minus(Duration.ofHours(80))
        );
    }

    private void createIssue(
            String title,
            String description,
            Category category,
            Severity severity,
            IssueStatus status,
            User assignee,
            boolean customerFacing,
            boolean productionImpact,
            int affectedUsers,
            Instant createdAt
    ) {
        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setCategory(category);
        issue.setSeverity(severity);
        issue.setStatus(status);
        issue.setAssignedUser(assignee);
        issue.setCustomerFacing(customerFacing);
        issue.setProductionImpact(productionImpact);
        issue.setAffectedUsers(affectedUsers);
        issue.setCreatedAt(createdAt);
        issue.setUpdatedAt(createdAt);
        TriageResult triage = triageService.calculate(issue);
        issue.setPriority(triage.priority());
        issue.setPriorityScore(triage.score());

        addHistory(issue, HistoryEventType.ISSUE_CREATED, null, status.name(), "Issue created", createdAt);
        if (status != IssueStatus.NEW) {
            addHistory(
                    issue,
                    HistoryEventType.STATUS_CHANGED,
                    IssueStatus.NEW.name(),
                    status.name(),
                    "Status changed from NEW to " + status,
                    createdAt.plus(Duration.ofMinutes(20))
            );
        }
        if (assignee != null) {
            addHistory(
                    issue,
                    HistoryEventType.ASSIGNEE_CHANGED,
                    null,
                    assignee.getName(),
                    "Assignee changed",
                    createdAt.plus(Duration.ofMinutes(10))
            );
        }
        issueRepository.save(issue);
    }

    private void addHistory(
            Issue issue,
            HistoryEventType eventType,
            String oldValue,
            String newValue,
            String description,
            Instant createdAt
    ) {
        IssueHistory history = new IssueHistory();
        history.setEventType(eventType);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setDescription(description);
        history.setCreatedAt(createdAt);
        issue.addHistory(history);
    }
}
