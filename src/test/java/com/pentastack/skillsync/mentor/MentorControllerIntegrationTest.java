package com.pentastack.skillsync.mentor;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pentastack.skillsync.domain.MentorProfile;
import com.pentastack.skillsync.domain.Role;
import com.pentastack.skillsync.domain.Stack;
import com.pentastack.skillsync.domain.User;
import com.pentastack.skillsync.domain.repository.MentorProfileRepository;
import com.pentastack.skillsync.domain.repository.StackRepository;
import com.pentastack.skillsync.domain.repository.StudentProfileRepository;
import com.pentastack.skillsync.domain.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MentorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private MentorProfileRepository mentorProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    private MentorProfile javaMentor;
    private MentorProfile reactMentor;
    private Stack javaStack;
    private Stack reactStack;

    @BeforeEach
    void setUp() {
        mentorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        stackRepository.deleteAll();
        userRepository.deleteAll();

        javaStack = stackRepository.save(new Stack("Java", "JVM development"));
        reactStack = stackRepository.save(new Stack("React", "Frontend engineering"));

        javaMentor = mentorProfileRepository.save(new MentorProfile(
            userRepository.save(User.create("java.mentor@skillsync.dev", "hash", Role.MENTOR)),
            javaStack,
            "Java Mentor",
            "Senior Java Engineer",
            "Enterprise JVM mentoring",
            true,
            4.9,
            BigDecimal.valueOf(150)
        ));

        reactMentor = mentorProfileRepository.save(new MentorProfile(
            userRepository.save(User.create("react.mentor@skillsync.dev", "hash", Role.MENTOR)),
            reactStack,
            "React Mentor",
            "Staff Frontend Engineer",
            "React and TypeScript mentoring",
            false,
            4.5,
            BigDecimal.valueOf(120)
        ));
    }

    @Test
    void listMentorsReturnsPaginatedEnvelopeWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/mentors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.items[0].name").exists())
            .andExpect(jsonPath("$.items[0].hourlyRate").exists());
    }

    @Test
    void listMentorsSupportsKeywordStackAndSortFilters() throws Exception {
        mockMvc.perform(get("/api/mentors")
                .param("keyword", "react")
                .param("stack", String.valueOf(reactStack.getId()))
                .param("sort_by", "price"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].name").value("React Mentor"));
    }

    @Test
    void listMentorsSupportsMultiStackFilter() throws Exception {
        mockMvc.perform(get("/api/mentors")
                .param("stack", javaStack.getId() + "," + reactStack.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void getMentorDetailReturnsProfileFields() throws Exception {
        mockMvc.perform(get("/api/mentors/{id}", javaMentor.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(javaMentor.getId()))
            .andExpect(jsonPath("$.title").value("Senior Java Engineer"))
            .andExpect(jsonPath("$.rating").value(4.9))
            .andExpect(jsonPath("$.hourlyRate").value(150))
            .andExpect(jsonPath("$.stacks", hasSize(1)))
            .andExpect(jsonPath("$.stacks[0].name").value("Java"));
    }

    @Test
    void getMentorDetailReturnsNotFoundForMissingId() throws Exception {
        mockMvc.perform(get("/api/mentors/{id}", 99999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Mentor not found"));
    }
}
