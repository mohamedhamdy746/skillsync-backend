package com.pentastack.skillsync.sessions;

import com.pentastack.skillsync.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SessionConflictException extends ApiException {
    public SessionConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
