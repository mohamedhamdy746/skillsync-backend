package com.pentastack.skillsync.sessions;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pentastack.skillsync.model.MentorProfile;
import com.pentastack.skillsync.model.Role;
import com.pentastack.skillsync.domain.ReviewSession;
import com.pentastack.skillsync.domain.SessionStatus;
import com.pentastack.skillsync.domain.Stack;
import com.pentastack.skillsync.model.StudentProfile;
import com.pentastack.skillsync.model.User;
import com.pentastack.skillsync.model.repository.MentorProfileRepository;
import com.pentastack.skillsync.domain.repository.ReviewSessionRepository;
import com.pentastack.skillsync.domain.repository.SessionAuditLogRepository;
import com.pentastack.skillsync.domain.repository.StackRepository;
import com.pentastack.skillsync.model.repository.StudentProfileRepository;
import com.pentastack.skillsync.model.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import com.pentastack.skillsync.domain.MentorAvailability;
import com.pentastack.skillsync.domain.repository.MentorAvailabilityRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private MentorProfileRepository mentorProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ReviewSessionRepository reviewSessionRepository;

    @Autowired
    private SessionAuditLogRepository sessionAuditLogRepository;

    @Autowired
    private MentorAvailabilityRepository mentorAvailabilityRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private FakeSessionAuditClassifier auditClassifier;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public FakeSessionAuditClassifier auditClassifier() {
            return new FakeSessionAuditClassifier();
        }
    }

    static class FakeSessionAuditClassifier implements SessionAuditClassifier {
        private AuditClassificationResult nextResult = AuditClassificationResult.success("GENERAL", 0.5, 1);
        private RuntimeException nextException = null;

        void returnNext(AuditClassificationResult nextResult) {
            this.nextResult = nextResult;
            this.nextException = null;
        }

        void throwNext(RuntimeException exception) {
            this.nextException = exception;
        }

        @Override
        public AuditClassificationResult classify(String submissionDescription) {
            if (nextException != null) {
                throw nextException;
            }
            return nextResult;
        }
    }

    private MentorProfile mentor;
    private MentorProfile otherMentor;
    private StudentProfile student;
    private StudentProfile otherStudent;
    private LocalDateTime firstSlot;

    @BeforeEach
    void setUp() {
        sessionAuditLogRepository.deleteAll();
        reviewSessionRepository.deleteAll();
        mentorAvailabilityRepository.deleteAll();
        mentorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        stackRepository.deleteAll();
        userRepository.deleteAll();

        Stack stack = stackRepository.save(new Stack("React Engineering", "Frontend competency reviews"));

        User mentorUser = userRepository.save(
            User.builder().email("mentor@skillsync.dev").passwordHash("hash").role(Role.MENTOR).build()
        );
        mentor = mentorProfileRepository.save(
            MentorProfile.builder()
                .user(mentorUser)
                .stack(stack)
                .name("Mona Mentor")
                .title("Senior React Mentor")
                .bio("Helps students debug UI systems")
                .available(true)
                .averageRating(4.8)
                .hourlyRate(BigDecimal.valueOf(60))
                .build()
        );

        User otherMentorUser = userRepository.save(
            User.builder().email("other.mentor@skillsync.dev").passwordHash("hash").role(Role.MENTOR).build()
        );
        otherMentor = mentorProfileRepository.save(
            MentorProfile.builder()
                .user(otherMentorUser)
                .stack(stack)
                .name("Other Mentor")
                .title("Backend Mentor")
                .bio("Helps students debug backend systems")
                .available(true)
                .averageRating(4.7)
                .hourlyRate(BigDecimal.valueOf(70))
                .build()
        );

        mentorAvailabilityRepository.save(
            new MentorAvailability(mentor, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 45))
        );
        mentorAvailabilityRepository.save(
            new MentorAvailability(mentor, DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 0))
        );
        mentorAvailabilityRepository.save(
            new MentorAvailability(mentor, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(12, 45))
        );
        mentorAvailabilityRepository.save(
            new MentorAvailability(mentor, DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(13, 45))
        );
        mentorAvailabilityRepository.save(
            new MentorAvailability(mentor, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(14, 45))
        );

        User studentUser = userRepository.save(
            User.builder().email("student@skillsync.dev").passwordHash("hash").role(Role.STUDENT).build()
        );
        student = studentProfileRepository.save(
            StudentProfile.builder().user(studentUser).name("Sam Student").build()
        );

        User otherStudentUser = userRepository.save(
            User.builder().email("other@skillsync.dev").passwordHash("hash").role(Role.STUDENT).build()
        );
        otherStudent = studentProfileRepository.save(
            StudentProfile.builder().user(otherStudentUser).name("Olive Other").build()
        );

        firstSlot = LocalDateTime.of(2026, 7, 6, 10, 0);
    }

    @Test
    void studentBooksSessionAndReceivesSuccessfulAuditLog() throws Exception {
        AuditClassificationResult auditResult = AuditClassificationResult.success("ASYNC_RACE", 0.91, 37);
        auditClassifier.returnNext(auditResult);

        mockMvc.perform(post("/api/sessions")
                .with(user("student@skillsync.dev").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "mentorId", mentor.getId(),
                    "startTime", firstSlot.toString(),
                    "description", "Reviewing an asynchronous race condition in my Node engine"
                ))))
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.mentorName").value("Mona Mentor"))
            .andExpect(jsonPath("$.studentName").value("Sam Student"))
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.audit.status").value("SUCCESS"))
            .andExpect(jsonPath("$.audit.predictedTag").value("ASYNC_RACE"))
            .andExpect(jsonPath("$.audit.confidenceScore").value(0.91))
            .andExpect(jsonPath("$.audit.latencyMs", notNullValue()));

        mockMvc.perform(get("/api/sessions")
                .with(user("student@skillsync.dev").roles("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].audit.status").value("SUCCESS"));
    }

    @Test
    void mentorCannotBookSessionThroughStudentBookingEndpoint() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .with(user("mentor@skillsync.dev").roles("MENTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "mentorId", mentor.getId(),
                    "startTime", firstSlot.toString(),
                    "description", "Mentor should be forbidden from booking"
                ))))
            .andExpect(status().isForbidden());
    }

    @Test
    void overlappingBookingForSameMentorIsRejected() throws Exception {
        auditClassifier.returnNext(AuditClassificationResult.success("REACT_STATE", 0.82, 21));

        bookAsStudent("student@skillsync.dev", firstSlot, "First review");

        mockMvc.perform(post("/api/sessions")
                .with(user("other@skillsync.dev").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "mentorId", mentor.getId(),
                    "startTime", firstSlot.plusMinutes(15).toString(),
                    "description", "Overlapping review"
                ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Mentor is already booked for that time window"));
    }

    @Test
    void classifierFailureIsStoredAsFailedAuditTelemetry() throws Exception {
        auditClassifier.returnNext(AuditClassificationResult.failed("classifier unavailable", 112));

        long sessionId = bookAsStudent("student@skillsync.dev", firstSlot, "Please inspect this flaky test");

        mockMvc.perform(get("/api/sessions/{id}/audit-log", sessionId)
                .with(user("student@skillsync.dev").roles("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.errorMessage").value("classifier unavailable"))
            .andExpect(jsonPath("$.latencyMs", notNullValue()));
    }

    @Test
    void classifierExceptionIsStoredAsFailedAuditTelemetryButBookingSucceeds() throws Exception {
        auditClassifier.throwNext(new RuntimeException("Simulated classifier error"));

        mockMvc.perform(post("/api/sessions")
                .with(user("student@skillsync.dev").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "mentorId", mentor.getId(),
                    "startTime", firstSlot.toString(),
                    "description", "Testing classifier exception"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.audit.status").value("FAILED"))
            .andExpect(jsonPath("$.audit.errorMessage").value("Simulated classifier error"))
            .andExpect(jsonPath("$.audit.latencyMs", notNullValue()));

        // Verify it was persisted in the DB as well
        mockMvc.perform(get("/api/sessions")
                .with(user("student@skillsync.dev").roles("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].audit.status").value("FAILED"))
            .andExpect(jsonPath("$[0].audit.errorMessage").value("Simulated classifier error"));
    }

    @Test
    void studentOnlySeesOwnSessionsAndCannotReadAnotherAuditLog() throws Exception {
        auditClassifier.returnNext(AuditClassificationResult.success("SYSTEM_DESIGN", 0.77, 19));

        long studentSessionId = bookAsStudent("student@skillsync.dev", firstSlot, "Student review");
        long otherSessionId = bookAsStudent("other@skillsync.dev", firstSlot.plusHours(2), "Other review");

        mockMvc.perform(get("/api/sessions")
                .with(user("student@skillsync.dev").roles("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(studentSessionId));

        mockMvc.perform(get("/api/sessions/{id}/audit-log", otherSessionId)
                .with(user("student@skillsync.dev").roles("STUDENT")))
            .andExpect(status().isForbidden());
    }

    @Test
    void mentorOnlySeesOwnSessions() throws Exception {
        ReviewSession ownSession = reviewSessionRepository.saveAndFlush(
            new ReviewSession(mentor, student, firstSlot, "Own mentor review")
        );
        reviewSessionRepository.saveAndFlush(
            new ReviewSession(otherMentor, student, firstSlot, "Other mentor review")
        );

        mockMvc.perform(get("/api/sessions")
                .with(user("mentor@skillsync.dev").roles("MENTOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(ownSession.getId()))
            .andExpect(jsonPath("$[0].mentorName").value("Mona Mentor"));
    }

    @Test
    void studentCannotWriteEvaluationNotes() throws Exception {
        long sessionId = bookAsStudent("student@skillsync.dev", firstSlot, "Review my PR");

        mockMvc.perform(put("/api/sessions/{id}", sessionId)
                .with(user("student@skillsync.dev").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "evaluationNotes", "Student should not write mentor notes"
                ))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Only the mentor or an admin can add evaluation notes"));
    }

    @Test
    void mentorCanSaveEvaluationNotesBeforeCompletion() throws Exception {
        long sessionId = bookAsStudent("student@skillsync.dev", firstSlot, "Review my PR");

        mockMvc.perform(put("/api/sessions/{id}", sessionId)
                .with(user("mentor@skillsync.dev").roles("MENTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "evaluationNotes", "Draft feedback before completion"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.evaluationNotes").value("Draft feedback before completion"));
    }

    @Test
    void mentorCompletesOwnSessionWithEvaluationNotes() throws Exception {
        auditClassifier.returnNext(AuditClassificationResult.success("CODE_REVIEW", 0.86, 24));

        long sessionId = bookAsStudent("student@skillsync.dev", firstSlot, "Review my PR");

        mockMvc.perform(put("/api/sessions/{id}", sessionId)
                .with(user("mentor@skillsync.dev").roles("MENTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "status", SessionStatus.COMPLETED.name(),
                    "evaluationNotes", "Strong debugging notes; practice naming state transitions."
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.evaluationNotes").value("Strong debugging notes; practice naming state transitions."));
    }

    @Test
    void studentCanCancelAndRescheduleOwnScheduledSession() throws Exception {
        auditClassifier.returnNext(AuditClassificationResult.success("API_DESIGN", 0.8, 28));

        long cancelId = bookAsStudent("student@skillsync.dev", firstSlot, "Cancel this review");

        mockMvc.perform(put("/api/sessions/{id}", cancelId)
                .with(user("student@skillsync.dev").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "status", SessionStatus.CANCELED.name()
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELED"));

        long rescheduleId = bookAsStudent("student@skillsync.dev", firstSlot.plusHours(3), "Move this review");
        LocalDateTime newTime = firstSlot.plusHours(4);

        mockMvc.perform(put("/api/sessions/{id}", rescheduleId)
                .with(user("student@skillsync.dev").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "startTime", newTime.toString()
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startTime").value("2026-07-06T14:00:00"))
            .andExpect(jsonPath("$.endTime").value("2026-07-06T14:45:00"));
    }

    @Test
    void reschedulingToSameStartTimeDoesNotConflictWithCurrentSession() throws Exception {
        auditClassifier.returnNext(AuditClassificationResult.success("API_DESIGN", 0.8, 28));

        long sessionId = bookAsStudent("student@skillsync.dev", firstSlot, "Keep this review at the same time");

        mockMvc.perform(put("/api/sessions/{id}", sessionId)
                .with(user("student@skillsync.dev").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "startTime", firstSlot.toString()
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startTime").value("2026-07-06T10:00:00"))
            .andExpect(jsonPath("$.endTime").value("2026-07-06T10:45:00"));
    }

    private long bookAsStudent(String email, LocalDateTime startTime, String description) throws Exception {
        String response = mockMvc.perform(post("/api/sessions")
                .with(user(email).roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "mentorId", mentor.getId(),
                    "startTime", startTime.toString(),
                    "description", description
                ))))
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
