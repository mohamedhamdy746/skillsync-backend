package com.pentastack.skillsync.availability;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pentastack.skillsync.availability.dto.AvailabilityRequest;
import com.pentastack.skillsync.availability.dto.AvailabilityResponse;
import com.pentastack.skillsync.availability.dto.AvailableSlotsResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mentors/{mentorId}")
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/availability")
    public List<AvailabilityResponse> list(@PathVariable Long mentorId) {
        return availabilityService.list(mentorId);
    }

    @GetMapping("/available-slots")
    public AvailableSlotsResponse availableSlots(
        @PathVariable Long mentorId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return availabilityService.availableSlots(mentorId, date);
    }

    @PostMapping("/availability")
    public ResponseEntity<AvailabilityResponse> create(
        @PathVariable Long mentorId,
        @Valid @RequestBody AvailabilityRequest request,
        Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(availabilityService.create(principal.getName(), mentorId, request));
    }

    @PutMapping("/availability/{availabilityId}")
    public AvailabilityResponse update(
        @PathVariable Long mentorId,
        @PathVariable Long availabilityId,
        @Valid @RequestBody AvailabilityRequest request,
        Principal principal
    ) {
        return availabilityService.update(principal.getName(), mentorId, availabilityId, request);
    }

    @DeleteMapping("/availability/{availabilityId}")
    public ResponseEntity<Void> delete(
        @PathVariable Long mentorId,
        @PathVariable Long availabilityId,
        Principal principal
    ) {
        availabilityService.delete(principal.getName(), mentorId, availabilityId);
        return ResponseEntity.noContent().build();
    }
}
