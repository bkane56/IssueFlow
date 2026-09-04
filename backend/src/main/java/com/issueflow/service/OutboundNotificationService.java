package com.issueflow.service;

import com.issueflow.constants.ErrorConstants;
import com.issueflow.constants.OutboundConstants;
import com.issueflow.dto.response.OutboundJobResponse;
import com.issueflow.entity.HistoryEventType;
import com.issueflow.entity.Issue;
import com.issueflow.entity.IssueHistory;
import com.issueflow.entity.IssueStatus;
import com.issueflow.entity.OutboundJob;
import com.issueflow.entity.OutboundJobStatus;
import com.issueflow.entity.OutboundOperationType;
import com.issueflow.exception.InvalidStateTransitionException;
import com.issueflow.exception.ResourceNotFoundException;
import com.issueflow.mapper.OutboundJobMapper;
import com.issueflow.repository.IssueRepository;
import com.issueflow.repository.OutboundJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class OutboundNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundNotificationService.class);

    private final IssueRepository issueRepository;
    private final OutboundJobRepository outboundJobRepository;
    private final OutboundJobMapper outboundJobMapper;
    private final Clock clock;

    public OutboundNotificationService(
            IssueRepository issueRepository,
            OutboundJobRepository outboundJobRepository,
            OutboundJobMapper outboundJobMapper,
            Clock clock
    ) {
        this.issueRepository = issueRepository;
        this.outboundJobRepository = outboundJobRepository;
        this.outboundJobMapper = outboundJobMapper;
        this.clock = clock;
    }

    public OutboundJobResponse enqueueEscalation(Long issueId) {
        Issue issue = getIssue(issueId);
        if (issue.getStatus() == IssueStatus.CLOSED) {
            throw new InvalidStateTransitionException(ErrorConstants.ESCALATION_NOT_ALLOWED_FOR_CLOSED_ISSUE);
        }

        String idempotencyKey = OutboundOperationType.ESCALATION_NOTIFICATION.idempotencyKey(issueId);
        return outboundJobRepository.findByIdempotencyKey(idempotencyKey)
                .map(outboundJobMapper::toResponse)
                .orElseGet(() -> createEscalationJob(issue, idempotencyKey));
    }

    @Transactional(readOnly = true)
    public List<OutboundJobResponse> findByIssueId(Long issueId) {
        getIssue(issueId);
        return outboundJobRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .map(outboundJobMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OutboundJobResponse findById(Long jobId) {
        return outboundJobRepository.findById(jobId)
                .map(outboundJobMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorConstants.OUTBOUND_JOB_NOT_FOUND.formatted(jobId)));
    }

    private OutboundJobResponse createEscalationJob(Issue issue, String idempotencyKey) {
        Instant now = Instant.now(clock);
        if (issue.getEscalationRequestedAt() == null) {
            issue.setEscalationRequestedAt(now);
        }
        issue.setUpdatedAt(now);
        addHistory(
                issue,
                HistoryEventType.ESCALATION_NOTIFICATION_QUEUED,
                null,
                idempotencyKey,
                OutboundConstants.HISTORY_QUEUED
        );
        issueRepository.save(issue);

        OutboundJob job = new OutboundJob();
        job.setOperationType(OutboundOperationType.ESCALATION_NOTIFICATION);
        job.setIssue(issue);
        job.setIdempotencyKey(idempotencyKey);
        job.setStatus(OutboundJobStatus.PENDING);
        job.setAttemptCount(0);
        job.setNextAttemptAt(now);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        try {
            OutboundJob saved = outboundJobRepository.save(job);
            LOGGER.info(
                    "Job created jobId={} issueId={} idempotencyKey={} attemptCount={}",
                    saved.getId(),
                    issue.getId(),
                    saved.getIdempotencyKey(),
                    saved.getAttemptCount()
            );
            return outboundJobMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            return outboundJobRepository.findByIdempotencyKey(idempotencyKey)
                    .map(outboundJobMapper::toResponse)
                    .orElseThrow(() -> exception);
        }
    }

    private Issue getIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorConstants.ISSUE_NOT_FOUND.formatted(issueId)));
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
        issue.getHistory().size();
        issue.addHistory(history);
    }
}
