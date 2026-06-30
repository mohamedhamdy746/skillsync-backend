package com.pentastack.skillsync.admin.dto;

public record AdminStudentListResponse(
    Long id,
    Long userId,
    String displayName,
    String email,
    boolean isBlocked,
    long totalSessions
) {}
