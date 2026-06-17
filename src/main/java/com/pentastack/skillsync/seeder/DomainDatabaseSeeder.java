package com.pentastack.skillsync.seeder;

import com.pentastack.skillsync.domain.MentorProfile;
import com.pentastack.skillsync.domain.Role;
import com.pentastack.skillsync.domain.Stack;
import com.pentastack.skillsync.domain.StudentProfile;
import com.pentastack.skillsync.domain.User;
import com.pentastack.skillsync.domain.repository.MentorProfileRepository;
import com.pentastack.skillsync.domain.repository.StackRepository;
import com.pentastack.skillsync.domain.repository.StudentProfileRepository;
import com.pentastack.skillsync.domain.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DomainDatabaseSeeder implements CommandLineRunner {

    private final UserRepository domainUserRepository;
    private final StudentProfileRepository domainStudentProfileRepository;
    private final MentorProfileRepository domainMentorProfileRepository;
    private final StackRepository stackRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Stack javaStack = ensureStack("Java", "Enterprise JVM development");
        Stack reactStack = ensureStack("React", "Modern frontend engineering");
        Stack goStack = ensureStack("Go", "High-performance backend systems");

        seedStudent();
        seedMentor("mentor@skillsync.com", "Test Mentor", javaStack, "Senior Distributed Systems Engineer",
            "Expert in high-performance distributed architecture, Go, and Java systems.", true, 4.9, "150.00");
        seedMentor("mentor.react@skillsync.com", "React Mentor", reactStack, "Staff Frontend Engineer",
            "Specializes in React, TypeScript, and design systems.", true, 4.7, "120.00");
        seedMentor("mentor.go@skillsync.com", "Go Mentor", goStack, "Platform Engineer",
            "Builds resilient microservices and observability pipelines.", false, 4.5, "135.00");
    }

    private Stack ensureStack(String name, String description) {
        return stackRepository.findAll().stream()
            .filter(stack -> stack.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> stackRepository.save(new Stack(name, description)));
    }

    private void seedStudent() {
        String email = "student@skillsync.com";
        if (domainUserRepository.existsByEmail(email)) {
            return;
        }

        User user = domainUserRepository.save(
            User.create(email, passwordEncoder.encode("student123"), Role.STUDENT)
        );
        domainStudentProfileRepository.save(new StudentProfile(user, "Test Student"));
        log.info("Seeded domain student user: {}", email);
    }

    private void seedMentor(
        String email,
        String displayName,
        Stack stack,
        String title,
        String bio,
        boolean available,
        double rating,
        String hourlyRate
    ) {
        if (domainUserRepository.existsByEmail(email)) {
            return;
        }

        User user = domainUserRepository.save(
            User.create(email, passwordEncoder.encode("mentor123"), Role.MENTOR)
        );
        domainMentorProfileRepository.save(new MentorProfile(
            user,
            stack,
            displayName,
            title,
            bio,
            available,
            rating,
            new BigDecimal(hourlyRate)
        ));
        log.info("Seeded domain mentor user: {}", email);
    }
}
