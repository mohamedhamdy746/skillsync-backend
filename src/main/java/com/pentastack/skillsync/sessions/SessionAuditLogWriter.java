package com.pentastack.skillsync.sessions;

import org.springframework.stereotype.Component;

import com.pentastack.skillsync.domain.SessionAuditLog;
import com.pentastack.skillsync.domain.repository.SessionAuditLogRepository;

@Component
public class SessionAuditLogWriter {
    private final SessionAuditLogRepository sessionAuditLogRepository;

    public SessionAuditLogWriter(SessionAuditLogRepository sessionAuditLogRepository) {
        this.sessionAuditLogRepository = sessionAuditLogRepository;
    }

    public SessionAuditLog saveAndFlush(SessionAuditLog auditLog) {
        return sessionAuditLogRepository.saveAndFlush(auditLog);
    }
}
