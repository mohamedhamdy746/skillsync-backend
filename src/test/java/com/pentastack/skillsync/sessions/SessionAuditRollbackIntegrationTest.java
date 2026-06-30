package com.pentastack.skillsync.sessions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pentastack.skillsync.domain.MentorAvailability;
import com.pentastack.skillsync.domain.SessionAuditLog;
import com.pentastack.skillsync.domain.Stack;
import com.pentastack.skillsync.domain.repository.MentorAvailabilityRepository;
import com.pentastack.skillsync.domain.repository.ReviewSessionRepository;
import com.pentastack.skillsync.domain.repository.StackRepository;
import com.pentastack.skillsync.model.MentorProfile;
import com.pentastack.skillsync.model.Role;
import com.pentastack.skillsync.model.StudentProfile;
import com.pentastack.skillsync.model.User;
import com.pentastack.skillsync.model.repository.MentorProfileRepository;
import com.pentastack.skillsync.model.repository.StudentProfileRepository;
import com.pentastack.skillsync.model.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
class SessionAuditRollbackIntegrationTest {

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
    private MentorAvailabilityRepository mentorAvailabilityRepository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        SessionAuditLogWriter failingSessionAuditLogWriter() {
            return new SessionAuditLogWriter(null) {
                @Override
                public SessionAuditLog saveAndFlush(SessionAuditLog auditLog) {
                    throw new IllegalStateException("audit persistence failed");
                }
            };
        }
    }

    private MentorProfile mentor;
    private LocalDateTime firstSlot;

    @BeforeEach
    void setUp() {
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

        mentorAvailabilityRepository.save(
            new MentorAvailability(mentor, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 45))
        );

        User studentUser = userRepository.save(
            User.builder().email("student@skillsync.dev").passwordHash("hash").role(Role.STUDENT).build()
        );
        studentProfileRepository.save(
            StudentProfile.builder().user(studentUser).name("Sam Student").build()
        );

        firstSlot = LocalDateTime.of(2026, 7, 6, 10, 0);
    }

    @Test
    void auditLogPersistenceFailureRollsBackSessionCreation() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .with(user("student@skillsync.dev").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "mentorId", mentor.getId(),
                    "startTime", firstSlot.toString(),
                    "description", "Review this booking atomically"
                ))))
            .andExpect(status().isInternalServerError());

        org.assertj.core.api.Assertions.assertThat(reviewSessionRepository.count()).isZero();
    }
}
