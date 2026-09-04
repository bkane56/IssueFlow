package com.issueflow.repository;

import com.issueflow.entity.OutboundJob;
import com.issueflow.entity.OutboundJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OutboundJobRepository extends JpaRepository<OutboundJob, Long> {

    Optional<OutboundJob> findByIdempotencyKey(String idempotencyKey);

    List<OutboundJob> findByIssueIdOrderByCreatedAtAsc(Long issueId);

    List<OutboundJob> findByStatusInAndNextAttemptAtLessThanEqual(
            Collection<OutboundJobStatus> statuses,
            Instant dueAt
    );

    List<OutboundJob> findByStatusAndUpdatedAtLessThan(
            OutboundJobStatus status,
            Instant staleBefore
    );
}
