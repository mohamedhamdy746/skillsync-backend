package com.pentastack.skillsync.domain.repository;

import com.pentastack.skillsync.domain.ReviewSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        SELECT COUNT(s) FROM ReviewSession s
        WHERE s.mentor.id = :mentorId
          AND s.status = :status
          AND s.id <> :excludedSessionId
          AND s.startTime < :endTime
          AND s.endTime > :startTime
        """)
    long countOverlappingSessionsExcludingId(
        @Param("mentorId") Long mentorId,
        @Param("status") com.pentastack.skillsync.domain.SessionStatus status,
        @Param("endTime") LocalDateTime endTime,
        @Param("startTime") LocalDateTime startTime,
        @Param("excludedSessionId") Long excludedSessionId
    );

    long countByMentor_Id(Long mentorId);

    List<ReviewSession> findByMentor_IdAndStatusAndStartTimeBetween(
        Long mentorId,
        com.pentastack.skillsync.domain.SessionStatus status,
        LocalDateTime start,
        LocalDateTime end
    );

    @EntityGraph(attributePaths = {"mentor", "mentor.user", "mentor.stack", "student", "student.user", "auditLog"})
    List<ReviewSession> findByStudent_User_IdOrderByStartTimeDesc(Long userId);

    @EntityGraph(attributePaths = {"mentor", "mentor.user", "mentor.stack", "student", "student.user", "auditLog"})
    List<ReviewSession> findByMentor_IdOrderByStartTimeDesc(Long mentorId);

    @Query("SELECT s.mentor.id, COUNT(s) FROM ReviewSession s GROUP BY s.mentor.id")
    List<Object[]> countSessionsGroupedByMentorId();

    @Query("SELECT s.student.user.id, COUNT(s) FROM ReviewSession s GROUP BY s.student.user.id")
    List<Object[]> countSessionsGroupedByStudentUserId();
}
