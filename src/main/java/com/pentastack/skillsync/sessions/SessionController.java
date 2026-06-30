package com.pentastack.skillsync.sessions;

import com.pentastack.skillsync.sessions.dto.CreateSessionRequest;
import com.pentastack.skillsync.sessions.dto.SessionAuditLogResponse;
import com.pentastack.skillsync.sessions.dto.SessionResponse;
import com.pentastack.skillsync.sessions.dto.UpdateSessionRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(principal.getName(), request));
    }

    @GetMapping
    public List<SessionResponse> list(Principal principal) {
        return sessionService.listSessions(principal.getName());
    }

    @GetMapping("/{id}")
    public SessionResponse get(@PathVariable Long id, Principal principal) {
        return sessionService.getSession(principal.getName(), id);
    }

    @PutMapping("/{id}")
    public SessionResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSessionRequest request, Principal principal) {
        return sessionService.updateSession(principal.getName(), id, request);
    }

    @GetMapping("/{id}/audit-log")
    public SessionAuditLogResponse audit(@PathVariable Long id, Principal principal) {
        return sessionService.getAuditLog(principal.getName(), id);
    }
}
