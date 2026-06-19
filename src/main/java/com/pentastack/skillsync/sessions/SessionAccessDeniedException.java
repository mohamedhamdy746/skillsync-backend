package com.pentastack.skillsync.sessions;

import com.pentastack.skillsync.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SessionAccessDeniedException extends ApiException {
    public SessionAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
