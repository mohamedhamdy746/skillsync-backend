package com.pentastack.skillsync.availability.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AvailableSlotsResponse(
    Long mentorId,
    LocalDate date,
    List<Slot> availableSlots
) {
    public record Slot(LocalDateTime startTime, LocalDateTime endTime) {}
}
