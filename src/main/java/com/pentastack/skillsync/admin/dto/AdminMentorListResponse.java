package com.pentastack.skillsync.admin.dto;

import java.math.BigDecimal;

public record AdminMentorListResponse(
    Long id,
    Long userId,
    String displayName,
    String email,
    String stackName,
    String title,
    String bio,
    boolean available,
    boolean isBlocked,
    Double rating,
    BigDecimal hourlyRate,
    long totalSessions
) {}
