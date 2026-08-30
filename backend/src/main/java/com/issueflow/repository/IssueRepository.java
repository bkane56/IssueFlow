package com.issueflow.repository;

import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.Severity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    @EntityGraph(attributePaths = "assignedUser")
    @Override
    Optional<Issue> findById(Long id);

    @EntityGraph(attributePaths = "assignedUser")
    @Override
    List<Issue> findAll(Specification<Issue> spec, Sort sort);

    long countByStatusNotIn(Collection<IssueStatus> statuses);

    long countBySeverityAndStatusNotIn(Severity severity, Collection<IssueStatus> statuses);

    long countByStatus(IssueStatus status);
}
