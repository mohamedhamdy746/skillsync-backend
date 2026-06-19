package com.pentastack.skillsync.sessions;

import com.pentastack.skillsync.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends ApiException {
    public SessionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
