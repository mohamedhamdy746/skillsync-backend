package com.pentastack.skillsync.admin;

import com.pentastack.skillsync.admin.dto.*;
import com.pentastack.skillsync.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
public class AdminController {

    private final AdminMentorService adminMentorService;

    public AdminController(AdminMentorService adminMentorService) {
        this.adminMentorService = adminMentorService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all platform users",
               description = "Returns all model users with account block status and role-specific verification flags")
    public ResponseEntity<List<AdminUserResponse>> getUsers() {
        return ResponseEntity.ok(adminMentorService.getUsers());
    }

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve or block a platform user",
               description = "Updates the model User account block flag from a status, active, or isBlocked payload")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
        @PathVariable Long id,
        @RequestBody AdminUserStatusRequest request
    ) {
        return ResponseEntity.ok(adminMentorService.updateUserStatus(id, request));
    }

    @GetMapping("/mentors/pending/registrations")
    @Operation(summary = "List pending mentor registrations",
               description = "Returns paginated list of unverified mentor registration profiles from the model schema")
    public ResponseEntity<PagedResponse<AdminRegistrationMentorResponse>> getPendingRegistrations(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminMentorService.getPendingRegistrations(page, size));
    }

    @PutMapping("/mentors/registrations/{id}/verification")
    @Operation(summary = "Verify or reject a mentor registration",
               description = "Updates the isVerified flag on the model MentorProfile by id")
    public ResponseEntity<Void> updateRegistrationVerification(
        @PathVariable Long id,
        @RequestBody Map<String, Boolean> body
    ) {
        boolean isVerified = Boolean.TRUE.equals(body.get("isVerified"));
        adminMentorService.updateRegistrationVerification(id, isVerified);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mentors/pending/live")
    @Operation(summary = "List pending live mentor verifications",
               description = "Returns paginated list of domain mentors where available is false")
    public ResponseEntity<PagedResponse<AdminLiveMentorResponse>> getPendingLiveVerifications(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminMentorService.getPendingLiveVerifications(page, size));
    }

    @PutMapping("/mentors/live/{id}/verification")
    @Operation(summary = "Approve or reject a live mentor")
    public ResponseEntity<Void> updateLiveVerification(
        @PathVariable Long id,
        @RequestBody Map<String, Boolean> body
    ) {
        boolean isAvailable = Boolean.TRUE.equals(body.get("isVerified"));
        adminMentorService.updateLiveVerification(id, isAvailable);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get admin dashboard stats",
               description = "Returns aggregate counts across both model and domain schemas")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminMentorService.getStats());
    }

    @GetMapping("/mentors")
    @Operation(summary = "List all domain mentors",
               description = "Returns paginated list of all mentors in the domain schema (available and unavailable)")
    public ResponseEntity<PagedResponse<AdminMentorListResponse>> getAllMentors(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminMentorService.getAllMentors(page, size));
    }

    @GetMapping("/mentors/{id}")
    @Operation(summary = "Get mentor detail",
               description = "Returns full details and session history for a domain mentor")
    public ResponseEntity<AdminMentorDetailResponse> getMentorDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminMentorService.getMentorDetail(id));
    }

    @GetMapping("/students")
    @Operation(summary = "List all domain students",
               description = "Returns paginated list of all students in the domain schema")
    public ResponseEntity<PagedResponse<AdminStudentListResponse>> getAllStudents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminMentorService.getAllStudents(page, size));
    }

    @GetMapping("/students/{id}")
    @Operation(summary = "Get student detail",
               description = "Returns full details and session history for a domain student")
    public ResponseEntity<AdminStudentDetailResponse> getStudentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminMentorService.getStudentDetail(id));
    }
}
