package com.pentastack.skillsync.mentor.dto;

import com.pentastack.skillsync.stack.dto.StackResponse;
import java.math.BigDecimal;
import java.util.List;

public record MentorDetailResponse(
    Long id,
    String name,
    String email,
    String title,
    String bio,
    Double rating,
    BigDecimal hourlyRate,
    boolean available,
    List<StackResponse> stacks,
    long totalSessions,
    boolean isApproved
) {}
