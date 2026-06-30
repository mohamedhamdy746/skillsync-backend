package com.pentastack.skillsync.sessions;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pentastack.skillsync.availability.AvailabilityService;
import com.pentastack.skillsync.domain.AuditStatus;
import com.pentastack.skillsync.model.MentorProfile;
import com.pentastack.skillsync.model.Role;
import com.pentastack.skillsync.domain.ReviewSession;
import com.pentastack.skillsync.domain.SessionAuditLog;
import com.pentastack.skillsync.domain.SessionStatus;
import com.pentastack.skillsync.model.StudentProfile;
import com.pentastack.skillsync.model.User;
import com.pentastack.skillsync.model.repository.MentorProfileRepository;
import com.pentastack.skillsync.domain.repository.ReviewSessionRepository;
import com.pentastack.skillsync.domain.repository.SessionAuditLogRepository;
import com.pentastack.skillsync.model.repository.StudentProfileRepository;
import com.pentastack.skillsync.model.repository.UserRepository;
import com.pentastack.skillsync.exception.ApiException;
import com.pentastack.skillsync.sessions.dto.CreateSessionRequest;
import com.pentastack.skillsync.sessions.dto.SessionAuditLogResponse;
import com.pentastack.skillsync.sessions.dto.SessionResponse;
import com.pentastack.skillsync.sessions.dto.UpdateSessionRequest;

@Service
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final ReviewSessionRepository reviewSessionRepository;
    private final SessionAuditLogRepository sessionAuditLogRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final SessionAuditClassifier classifier;
    private final SessionAuditLogWriter sessionAuditLogWriter;
    private final AvailabilityService availabilityService;

    public SessionService(
        ReviewSessionRepository reviewSessionRepository,
        SessionAuditLogRepository sessionAuditLogRepository,
        MentorProfileRepository mentorProfileRepository,
        StudentProfileRepository studentProfileRepository,
        UserRepository userRepository,
        SessionAuditClassifier classifier,
        SessionAuditLogWriter sessionAuditLogWriter,
        AvailabilityService availabilityService
    ) {
        this.reviewSessionRepository = reviewSessionRepository;
        this.sessionAuditLogRepository = sessionAuditLogRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.classifier = classifier;
        this.sessionAuditLogWriter = sessionAuditLogWriter;
        this.availabilityService = availabilityService;
    }

    @Transactional
    public SessionResponse createSession(String studentEmail, CreateSessionRequest request) {
        StudentProfile student = studentProfileRepository.findByUser_Email(studentEmail)
            .orElseThrow(() -> new SessionNotFoundException("Student profile not found"));
        MentorProfile mentor = mentorProfileRepository.findWithUserById(request.mentorId())
            .orElseThrow(() -> new SessionNotFoundException("Mentor profile not found"));

        LocalDateTime endTime = request.startTime().plusMinutes(45);
        if (reviewSessionRepository.existsByMentor_IdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            mentor.getId(), SessionStatus.SCHEDULED, endTime, request.startTime()
        )) {
            throw new SessionConflictException("Mentor is already booked for that time window");
        }
        requireAvailableSlot(mentor.getId(), request.startTime());

        ReviewSession session;
        try {
            session = reviewSessionRepository.saveAndFlush(
                new ReviewSession(mentor, student, request.startTime(), request.description()));
        } catch (DataIntegrityViolationException ex) {
            throw new SessionConflictException("Mentor is already booked for that time window");
        }

        long startTime = System.currentTimeMillis();
        AuditClassificationResult classification = null;
        Throwable classifierEx = null;
        try {
            classification = classifier.classify(request.description());
        } catch (Throwable ex) {
            classifierEx = ex;
        }
        long latency = System.currentTimeMillis() - startTime;

        SessionAuditLog auditLog;
        if (classifierEx == null && classification != null) {
            auditLog = new SessionAuditLog(
                session,
                classification.predictedTag(),
                classification.confidenceScore(),
                classification.successful() ? AuditStatus.SUCCESS : AuditStatus.FAILED,
                classification.errorMessage(),
                latency
            );
        } else {
            String errorMsg = classifierEx != null ? classifierEx.getMessage() : "Classifier returned null result";
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            auditLog = new SessionAuditLog(
                session,
                null,
                null,
                AuditStatus.FAILED,
                errorMsg,
                latency
            );
        }

        SessionAuditLog savedAuditLog = sessionAuditLogWriter.saveAndFlush(auditLog);

        return toResponse(session, savedAuditLog);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
            .orElseThrow(() -> new SessionNotFoundException("User not found"));
        return switch (requester.getRole()) {
            case ADMIN -> reviewSessionRepository.findAllByOrderByStartTimeDesc().stream().map(this::toResponse).toList();
            case MENTOR -> reviewSessionRepository.findByMentor_User_EmailOrderByStartTimeDesc(requesterEmail).stream().map(this::toResponse).toList();
            case STUDENT -> reviewSessionRepository.findByStudent_User_EmailOrderByStartTimeDesc(requesterEmail).stream().map(this::toResponse).toList();
        };
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(String requesterEmail, Long sessionId) {
        ReviewSession session = reviewSessionRepository.findWithDetailsById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException("Session not found"));
        authorize(requesterEmail, session);
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public SessionAuditLogResponse getAuditLog(String requesterEmail, Long sessionId) {
        ReviewSession session = reviewSessionRepository.findWithDetailsById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException("Session not found"));
        authorize(requesterEmail, session);
        SessionAuditLog auditLog = sessionAuditLogRepository.findBySession_Id(sessionId)
            .orElseThrow(() -> new SessionNotFoundException("Audit log not found"));
        return toAuditResponse(auditLog);
    }

    @Transactional
    public SessionResponse updateSession(String requesterEmail, Long sessionId, UpdateSessionRequest request) {
        ReviewSession session = reviewSessionRepository.findWithDetailsById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException("Session not found"));
        User requester = authorize(requesterEmail, session);
        boolean noteUpdate = request.evaluationNotes() != null;
        boolean completion = request.status() == SessionStatus.COMPLETED;

        if (noteUpdate || completion) {
            requireMentorOrAdmin(requesterEmail, requester, session);
        }

        boolean schedulingUpdate = request.startTime() != null
            || request.description() != null
            || request.status() == SessionStatus.CANCELED
            || completion;
        if (schedulingUpdate && session.getStatus() != SessionStatus.SCHEDULED) {
            throw new SessionConflictException("Only scheduled sessions can be updated");
        }

        if (request.startTime() != null && !request.startTime().equals(session.getStartTime())) {
            LocalDateTime endTime = request.startTime().plusMinutes(45);
            boolean overlap = reviewSessionRepository.countOverlappingSessionsExcludingId(
                session.getMentor().getId(), SessionStatus.SCHEDULED, endTime, request.startTime(), session.getId()
            ) > 0;
            if (overlap) {
                throw new SessionConflictException("Mentor is already booked for that time window");
            }
            requireAvailableSlot(session.getMentor().getId(), request.startTime());
            session.reschedule(request.startTime());
        }
        if (request.description() != null) {
            session.updateDescription(request.description());
        }
        if (request.status() == SessionStatus.CANCELED) {
            session.cancel();
        } else if (request.status() == SessionStatus.COMPLETED) {
            session.complete(request.evaluationNotes());
        } else if (noteUpdate) {
            session.updateEvaluationNotes(request.evaluationNotes());
        }
        try {
            reviewSessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException ex) {
            throw new SessionConflictException("Mentor is already booked for that time window");
        }
        return toResponse(session);
    }

    private void requireAvailableSlot(Long mentorId, LocalDateTime startTime) {
        boolean available = availabilityService.availableSlots(mentorId, startTime.toLocalDate())
            .availableSlots().stream()
            .anyMatch(slot -> slot.startTime().equals(startTime));
        if (!available) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Requested time is not an available slot");
        }
    }

    private User authorize(String requesterEmail, ReviewSession session) {
        User requester = userRepository.findByEmail(requesterEmail)
            .orElseThrow(() -> new SessionNotFoundException("User not found"));
        boolean owns = session.getStudent().getUser().getEmail().equals(requesterEmail)
            || session.getMentor().getUser().getEmail().equals(requesterEmail)
            || requester.getRole() == Role.ADMIN;
        if (!owns) {
            throw new SessionAccessDeniedException("You do not have access to this session");
        }
        return requester;
    }

    private void requireMentorOrAdmin(String requesterEmail, User requester, ReviewSession session) {
        boolean canEvaluate = session.getMentor().getUser().getEmail().equals(requesterEmail)
            || requester.getRole() == Role.ADMIN;
        if (!canEvaluate) {
            throw new SessionAccessDeniedException("Only the mentor or an admin can add evaluation notes");
        }
    }

    private SessionResponse toResponse(ReviewSession session) {
        SessionAuditLog audit = session.getAuditLog();
        return new SessionResponse(
            session.getId(),
            session.getMentor().getId(),
            session.getMentor().getName(),
            session.getStudent().getId(),
            session.getStudent().getName(),
            session.getStartTime(),
            session.getEndTime(),
            session.getDescription(),
            session.getStatus(),
            session.getEvaluationNotes(),
            audit == null ? null : toAuditResponse(audit)
        );
    }

    private SessionResponse toResponse(ReviewSession session, SessionAuditLog auditLog) {
        return new SessionResponse(
            session.getId(),
            session.getMentor().getId(),
            session.getMentor().getName(),
            session.getStudent().getId(),
            session.getStudent().getName(),
            session.getStartTime(),
            session.getEndTime(),
            session.getDescription(),
            session.getStatus(),
            session.getEvaluationNotes(),
            toAuditResponse(auditLog)
        );
    }

    private SessionAuditLogResponse toAuditResponse(SessionAuditLog auditLog) {
        return new SessionAuditLogResponse(
            auditLog.getId(),
            auditLog.getPredictedTag(),
            auditLog.getConfidenceScore(),
            auditLog.getStatus(),
            auditLog.getErrorMessage(),
            auditLog.getLatencyMs()
        );
    }
}
