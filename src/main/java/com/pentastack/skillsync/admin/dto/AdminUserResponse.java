package com.pentastack.skillsync.admin.dto;

import com.pentastack.skillsync.model.Role;
import java.time.LocalDateTime;

public record AdminUserResponse(
    Long id,
    String name,
    String email,
    Role role,
    Long profileId,
    LocalDateTime createdAt,
    boolean isApproved,
    boolean isBlocked,
    String status
) {}
