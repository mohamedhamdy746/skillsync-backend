package com.pentastack.skillsync.stack;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pentastack.skillsync.domain.Stack;
import com.pentastack.skillsync.domain.repository.MentorProfileRepository;
import com.pentastack.skillsync.domain.repository.StackRepository;
import com.pentastack.skillsync.domain.repository.StudentProfileRepository;
import com.pentastack.skillsync.domain.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StackControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private MentorProfileRepository mentorProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mentorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        userRepository.deleteAll();
        stackRepository.deleteAll();
        stackRepository.save(new Stack("Java", "JVM development"));
    }

    @Test
    void listStacksIsPublic() throws Exception {
        mockMvc.perform(get("/api/stacks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test
    void adminCanCreateUpdateAndDeleteStack() throws Exception {
        mockMvc.perform(post("/api/stacks")
                .with(user("admin@skillsync.com").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "React",
                    "description", "Frontend engineering"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("React"));

        String listResponse = mockMvc.perform(get("/api/stacks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long reactId = objectMapper.readTree(listResponse).get(1).get("id").asLong();

        mockMvc.perform(put("/api/stacks/{id}", reactId)
                .with(user("admin@skillsync.com").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "React Native",
                    "description", "Cross-platform UI"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("React Native"));

        mockMvc.perform(delete("/api/stacks/{id}", reactId)
                .with(user("admin@skillsync.com").roles("ADMIN")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stacks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void duplicateStackNameReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/stacks")
                .with(user("admin@skillsync.com").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "java",
                    "description", "Duplicate"
                ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("A stack with this name already exists"));
    }

    @Test
    void nonAdminCannotCreateStack() throws Exception {
        mockMvc.perform(post("/api/stacks")
                .with(user("student@skillsync.com").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Go",
                    "description", "Backend"
                ))))
            .andExpect(status().isForbidden());
    }
}
