package com.pentastack.skillsync.domain.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pentastack.skillsync.domain.MentorAvailability;

public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {

    List<MentorAvailability> findByMentor_IdOrderByDayOfWeekAscStartTimeAsc(Long mentorId);

    List<MentorAvailability> findByMentor_IdAndDayOfWeek(Long mentorId, DayOfWeek dayOfWeek);
}
