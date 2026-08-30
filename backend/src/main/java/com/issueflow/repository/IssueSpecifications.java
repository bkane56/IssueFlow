package com.issueflow.repository;

import com.issueflow.entity.Category;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Priority;
import com.issueflow.entity.Severity;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class IssueSpecifications {

    private IssueSpecifications() {
    }

    public static Specification<Issue> withFilters(
            IssueStatus status,
            Priority priority,
            Severity severity,
            Category category,
            Long assignedUserId,
            String search
    ) {
        return Specification.allOf(
                hasStatus(status),
                hasPriority(priority),
                hasSeverity(severity),
                hasCategory(category),
                hasAssignedUser(assignedUserId),
                matchesSearch(search)
        );
    }

    private static Specification<Issue> hasStatus(IssueStatus status) {
        return (root, query, builder) -> status == null ? null : builder.equal(root.get("status"), status);
    }

    private static Specification<Issue> hasPriority(Priority priority) {
        return (root, query, builder) -> priority == null ? null : builder.equal(root.get("priority"), priority);
    }

    private static Specification<Issue> hasSeverity(Severity severity) {
        return (root, query, builder) -> severity == null ? null : builder.equal(root.get("severity"), severity);
    }

    private static Specification<Issue> hasCategory(Category category) {
        return (root, query, builder) -> category == null ? null : builder.equal(root.get("category"), category);
    }

    private static Specification<Issue> hasAssignedUser(Long assignedUserId) {
        return (root, query, builder) -> {
            if (assignedUserId == null) {
                return null;
            }
            return builder.equal(root.join("assignedUser", JoinType.LEFT).get("id"), assignedUserId);
        };
    }

    private static Specification<Issue> matchesSearch(String search) {
        return (root, query, builder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("title")), pattern),
                    builder.like(builder.lower(root.get("description")), pattern)
            );
        };
    }
}
