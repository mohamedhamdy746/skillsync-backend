package com.pentastack.skillsync.availability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pentastack.skillsync.availability.dto.AvailabilityRequest;
import com.pentastack.skillsync.availability.dto.AvailabilityResponse;
import com.pentastack.skillsync.availability.dto.AvailableSlotsResponse;
import com.pentastack.skillsync.domain.MentorAvailability;
import com.pentastack.skillsync.domain.MentorProfile;
import com.pentastack.skillsync.domain.ReviewSession;
import com.pentastack.skillsync.domain.Role;
import com.pentastack.skillsync.domain.SessionStatus;
import com.pentastack.skillsync.domain.User;
import com.pentastack.skillsync.domain.repository.MentorAvailabilityRepository;
import com.pentastack.skillsync.domain.repository.MentorProfileRepository;
import com.pentastack.skillsync.domain.repository.ReviewSessionRepository;
import com.pentastack.skillsync.domain.repository.UserRepository;
import com.pentastack.skillsync.exception.ApiException;

@Service
public class AvailabilityService {
    private static final int SLOT_MINUTES = 45;

    private final MentorAvailabilityRepository availabilityRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final ReviewSessionRepository reviewSessionRepository;
    private final UserRepository userRepository;

    public AvailabilityService(
        MentorAvailabilityRepository availabilityRepository,
        MentorProfileRepository mentorProfileRepository,
        ReviewSessionRepository reviewSessionRepository,
        UserRepository userRepository
    ) {
        this.availabilityRepository = availabilityRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.reviewSessionRepository = reviewSessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> list(Long mentorId) {
        if (!mentorProfileRepository.existsById(mentorId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Mentor not found");
        }
        return availabilityRepository.findByMentor_IdOrderByDayOfWeekAscStartTimeAsc(mentorId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AvailableSlotsResponse availableSlots(Long mentorId, LocalDate date) {
        if (!mentorProfileRepository.existsById(mentorId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Mentor not found");
        }
        List<MentorAvailability> windows =
            availabilityRepository.findByMentor_IdAndDayOfWeek(mentorId, date.getDayOfWeek());
        List<ReviewSession> booked = reviewSessionRepository.findByMentor_IdAndStatusAndStartTimeBetween(
            mentorId, SessionStatus.SCHEDULED, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        LocalDateTime now = LocalDateTime.now();

        List<AvailableSlotsResponse.Slot> slots = new ArrayList<>();
        for (MentorAvailability w : windows) {
            LocalDateTime windowEnd = date.atTime(w.getEndTime());
            for (LocalDateTime start = date.atTime(w.getStartTime());
                 !start.plusMinutes(SLOT_MINUTES).isAfter(windowEnd);
                 start = start.plusMinutes(SLOT_MINUTES)) {
                LocalDateTime end = start.plusMinutes(SLOT_MINUTES);
                if (!start.isAfter(now)) continue;
                LocalDateTime slotStart = start, slotEnd = end;
                boolean taken = booked.stream().anyMatch(b ->
                    slotStart.isBefore(b.getEndTime()) && slotEnd.isAfter(b.getStartTime()));
                if (!taken) slots.add(new AvailableSlotsResponse.Slot(start, end));
            }
        }
        slots.sort(Comparator.comparing(AvailableSlotsResponse.Slot::startTime));
        return new AvailableSlotsResponse(mentorId, date, slots);
    }

    @Transactional
    public AvailabilityResponse create(String requesterEmail, Long mentorId, AvailabilityRequest req) {
        MentorProfile mentor = requireOwnedMentor(requesterEmail, mentorId);
        validateTimes(req);
        validateNoOverlap(mentorId, req.dayOfWeek(), req.startTime(), req.endTime(), null);
        MentorAvailability saved = availabilityRepository.save(
            new MentorAvailability(mentor, req.dayOfWeek(), req.startTime(), req.endTime()));
        return toResponse(saved);
    }

    @Transactional
    public AvailabilityResponse update(String requesterEmail, Long mentorId, Long availabilityId, AvailabilityRequest req) {
        requireOwnedMentor(requesterEmail, mentorId);
        MentorAvailability window = requireWindow(mentorId, availabilityId);
        validateTimes(req);
        validateNoOverlap(mentorId, req.dayOfWeek(), req.startTime(), req.endTime(), availabilityId);
        window.updateWindow(req.dayOfWeek(), req.startTime(), req.endTime());
        return toResponse(window);
    }

    @Transactional
    public void delete(String requesterEmail, Long mentorId, Long availabilityId) {
        requireOwnedMentor(requesterEmail, mentorId);
        availabilityRepository.delete(requireWindow(mentorId, availabilityId));
    }

    private MentorProfile requireOwnedMentor(String email, Long mentorId) {
        MentorProfile mentor = mentorProfileRepository.findWithUserById(mentorId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor not found"));
        User requester = userRepository.findByEmail(email)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        boolean owns = mentor.getUser().getEmail().equals(email) || requester.getRole() == Role.ADMIN;
        if (!owns) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage your own availability");
        }
        return mentor;
    }

    private MentorAvailability requireWindow(Long mentorId, Long availabilityId) {
        MentorAvailability window = availabilityRepository.findById(availabilityId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Availability window not found"));
        if (!window.getMentor().getId().equals(mentorId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Availability window not found");
        }
        return window;
    }

    private void validateTimes(AvailabilityRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
    }

    private void validateNoOverlap(Long mentorId, DayOfWeek day, LocalTime start, LocalTime end, Long excludeId) {
        for (MentorAvailability w : availabilityRepository.findByMentor_IdAndDayOfWeek(mentorId, day)) {
            if (excludeId != null && w.getId().equals(excludeId)) continue;
            if (start.isBefore(w.getEndTime()) && end.isAfter(w.getStartTime())) {
                throw new ApiException(HttpStatus.CONFLICT, "Window overlaps an existing one on " + day);
            }
        }
    }

    private AvailabilityResponse toResponse(MentorAvailability w) {
        return new AvailabilityResponse(w.getId(), w.getDayOfWeek(), w.getStartTime(), w.getEndTime());
    }
}
