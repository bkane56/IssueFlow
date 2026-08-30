package com.issueflow.repository;

import com.issueflow.entity.IssueHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueHistoryRepository extends JpaRepository<IssueHistory, Long> {

    List<IssueHistory> findByIssueIdOrderByCreatedAtAsc(Long issueId);
}
