package com.pentastack.skillsync.mentor.dto;

import java.math.BigDecimal;

public record MentorProfileUpdateRequest(
    String title,
    String bio,
    BigDecimal hourlyRate,
    boolean available
) {}
