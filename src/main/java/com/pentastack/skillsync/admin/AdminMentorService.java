package com.pentastack.skillsync.admin;

import com.pentastack.skillsync.admin.dto.*;
import com.pentastack.skillsync.common.dto.PagedResponse;
import com.pentastack.skillsync.domain.ReviewSession;
import com.pentastack.skillsync.model.StudentProfile;
import com.pentastack.skillsync.model.MentorProfile;
import com.pentastack.skillsync.model.Role;
import com.pentastack.skillsync.model.User;
import com.pentastack.skillsync.domain.repository.ReviewSessionRepository;
import com.pentastack.skillsync.domain.repository.StackRepository;
import com.pentastack.skillsync.exception.ApiException;
import com.pentastack.skillsync.model.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMentorService {

    private final com.pentastack.skillsync.model.repository.MentorProfileRepository mentorProfileRepository;
    private final com.pentastack.skillsync.model.repository.StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final StackRepository stackRepository;
    private final ReviewSessionRepository reviewSessionRepository;

    public AdminMentorService(
        com.pentastack.skillsync.model.repository.MentorProfileRepository mentorProfileRepository,
        com.pentastack.skillsync.model.repository.StudentProfileRepository studentProfileRepository,
        UserRepository userRepository,
        StackRepository stackRepository,
        ReviewSessionRepository reviewSessionRepository
    ) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.stackRepository = stackRepository;
        this.reviewSessionRepository = reviewSessionRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers() {
        return userRepository.findAll().stream()
            .map(this::toAdminUserResponse)
            .toList();
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long id, AdminUserStatusRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        user.setBlocked(resolveBlocked(request));
        return toAdminUserResponse(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminRegistrationMentorResponse> getPendingRegistrations(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<MentorProfile> result =
            mentorProfileRepository.findByIsVerifiedAndUser_Blocked(false, false, pageable);

        List<AdminRegistrationMentorResponse> items = result.getContent().stream()
            .map(mp -> new AdminRegistrationMentorResponse(
                mp.getId(),
                mp.getName(),
                mp.getUser().getEmail(),
                mp.getStack() != null ? mp.getStack().getName() : "Unknown stack",
                mp.getUser().getCreatedAt(),
                mp.isVerified()
            ))
            .toList();

        return new PagedResponse<>(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional
    public void updateRegistrationVerification(Long id, boolean isVerified) {
        MentorProfile mentor = mentorProfileRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Registration mentor not found"));
        mentor.setVerified(isVerified);
        if (isVerified) {
            mentor.getUser().setBlocked(false);
            mentor.setAvailable(true);
        } else {
            mentor.setAvailable(false);
            mentor.getUser().setBlocked(true);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminLiveMentorResponse> getPendingLiveVerifications(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<MentorProfile> result =
            mentorProfileRepository.findByAvailable(false, pageable);

        List<AdminLiveMentorResponse> items = result.getContent().stream()
            .map(mp -> new AdminLiveMentorResponse(
                mp.getId(),
                mp.getDisplayName(),
                mp.getUser().getEmail(),
                mp.getStack() != null ? mp.getStack().getName() : "Unknown stack",
                mp.isAvailable(),
                mp.getAverageRating()
            ))
            .toList();

        return new PagedResponse<>(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional
    public void updateLiveVerification(Long id, boolean isAvailable) {
        MentorProfile mentor = mentorProfileRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Live mentor not found"));
        mentor.updateProfile(mentor.getTitle(), mentor.getBio(), mentor.getHourlyRate(), isAvailable);
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalSessions = reviewSessionRepository.count();
        long activeMentors = mentorProfileRepository.countByAvailable(true);
        long pendingLiveVerifications = mentorProfileRepository.countByAvailable(false);
        long pendingRegistrations = mentorProfileRepository.countByIsVerifiedAndUser_Blocked(false, false);
        Double avgRating = mentorProfileRepository.findAverageRatingByAvailable(true).orElse(null);

        return new AdminStatsResponse(
            totalSessions,
            activeMentors,
            pendingLiveVerifications,
            pendingRegistrations,
            avgRating
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminMentorListResponse> getAllMentors(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<MentorProfile> result =
            mentorProfileRepository.findAllWithDetails(pageable);

        Map<Long, Long> sessionCounts = reviewSessionRepository.countSessionsGroupedByMentorId()
            .stream()
            .collect(Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        List<AdminMentorListResponse> items = result.getContent().stream()
            .map(mp -> new AdminMentorListResponse(
                mp.getId(),
                mp.getUser().getId(),
                mp.getDisplayName(),
                mp.getUser().getEmail(),
                mp.getStack() != null ? mp.getStack().getName() : "Unknown stack",
                mp.getTitle(),
                mp.getBio(),
                mp.isAvailable(),
                mp.getUser().isBlocked(),
                mp.getAverageRating(),
                mp.getHourlyRate(),
                sessionCounts.getOrDefault(mp.getId(), 0L)
            ))
            .toList();

        return new PagedResponse<>(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminMentorDetailResponse getMentorDetail(Long id) {
        MentorProfile mp = mentorProfileRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor not found"));

        List<ReviewSession> sessions = reviewSessionRepository.findByMentor_IdOrderByStartTimeDesc(id);

        return new AdminMentorDetailResponse(
            mp.getId(),
            mp.getDisplayName(),
            mp.getUser().getEmail(),
            mp.getStack() != null ? mp.getStack().getName() : "Unknown stack",
            mp.getStack() != null ? mp.getStack().getId() : null,
            mp.getTitle(),
            mp.getBio(),
            mp.isAvailable(),
            mp.getAverageRating(),
            mp.getHourlyRate(),
            sessions.size(),
            sessions.stream().map(s -> new AdminSessionSummaryResponse(
                s.getId(),
                s.getMentor().getDisplayName(),
                s.getStudent().getDisplayName(),
                s.getStudent().getUser().getEmail(),
                s.getStartTime().toString(),
                s.getEndTime().toString(),
                s.getStatus().name(),
                s.getDescription()
            )).toList()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminStudentListResponse> getAllStudents(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<StudentProfile> result = studentProfileRepository.findAllWithUser(pageable);

        Map<Long, Long> sessionCounts = reviewSessionRepository.countSessionsGroupedByStudentUserId()
            .stream()
            .collect(Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        List<AdminStudentListResponse> items = result.getContent().stream()
            .map(sp -> new AdminStudentListResponse(
                sp.getId(),
                sp.getUser().getId(),
                sp.getDisplayName(),
                sp.getUser().getEmail(),
                sp.getUser().isBlocked(),
                sessionCounts.getOrDefault(sp.getUser().getId(), 0L)
            ))
            .toList();

        return new PagedResponse<>(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminStudentDetailResponse getStudentDetail(Long id) {
        StudentProfile sp = studentProfileRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));

        List<ReviewSession> sessions = reviewSessionRepository.findByStudent_User_IdOrderByStartTimeDesc(sp.getUser().getId());

        return new AdminStudentDetailResponse(
            sp.getId(),
            sp.getDisplayName(),
            sp.getUser().getEmail(),
            sessions.size(),
            sessions.stream().map(s -> new AdminSessionSummaryResponse(
                s.getId(),
                s.getMentor().getDisplayName(),
                s.getStudent().getDisplayName(),
                s.getStudent().getUser().getEmail(),
                s.getStartTime().toString(),
                s.getEndTime().toString(),
                s.getStatus().name(),
                s.getDescription()
            )).toList()
        );
    }

    private boolean resolveBlocked(AdminUserStatusRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status payload is required");
        }
        if (request.status() != null) {
            return switch (request.status().trim().toUpperCase()) {
                case "BLOCKED" -> true;
                case "APPROVED", "ACTIVE" -> false;
                default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported user status: " + request.status());
            };
        }
        if (request.isBlocked() != null) {
            return request.isBlocked();
        }
        if (request.active() != null) {
            return !request.active();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Status, active, or isBlocked is required");
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        String name = "Admin User";
        Long profileId = null;
        boolean verified = !user.isBlocked();

        if (user.getRole() == Role.STUDENT && user.getStudentProfile() != null) {
            name = user.getStudentProfile().getDisplayName();
            profileId = user.getStudentProfile().getId();
        } else if (user.getRole() == Role.MENTOR && user.getMentorProfile() != null) {
            name = user.getMentorProfile().getDisplayName();
            profileId = user.getMentorProfile().getId();
            verified = user.getMentorProfile().isVerified();
        }

        boolean approved = verified && !user.isBlocked();
        return new AdminUserResponse(
            user.getId(),
            name,
            user.getEmail(),
            user.getRole(),
            profileId,
            user.getCreatedAt(),
            approved,
            user.isBlocked(),
            user.isBlocked() ? "BLOCKED" : "APPROVED"
        );
    }
}
