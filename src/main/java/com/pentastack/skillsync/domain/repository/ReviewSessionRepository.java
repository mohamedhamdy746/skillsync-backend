package com.pentastack.skillsync.domain.repository;

import com.pentastack.skillsync.domain.ReviewSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSessionRepository extends JpaRepository<ReviewSession, Long> {
    @EntityGraph(attributePaths = {"mentor", "mentor.user", "mentor.stack", "student", "student.user", "auditLog"})
    List<ReviewSession> findByStudent_User_EmailOrderByStartTimeDesc(String email);

    @EntityGraph(attributePaths = {"mentor", "mentor.user", "mentor.stack", "student", "student.user", "auditLog"})
    List<ReviewSession> findByMentor_User_EmailOrderByStartTimeDesc(String email);

    @EntityGraph(attributePaths = {"mentor", "mentor.user", "mentor.stack", "student", "student.user", "auditLog"})
    List<ReviewSession> findAllByOrderByStartTimeDesc();

    @EntityGraph(attributePaths = {"mentor", "mentor.user", "mentor.stack", "student", "student.user", "auditLog"})
    Optional<ReviewSession> findWithDetailsById(Long id);

    boolean existsByMentor_IdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
        Long mentorId,
        com.pentastack.skillsync.domain.SessionStatus status,
        LocalDateTime endTime,
        LocalDateTime startTime
    );

    long countByMentor_Id(Long mentorId);
}
