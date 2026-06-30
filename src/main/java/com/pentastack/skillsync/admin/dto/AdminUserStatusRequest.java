package com.pentastack.skillsync.admin.dto;

public record AdminUserStatusRequest(
    String status,
    Boolean active,
    Boolean isBlocked
) {}
