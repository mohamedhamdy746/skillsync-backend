package com.pentastack.skillsync.dto;

import com.pentastack.skillsync.model.Role;
import com.pentastack.skillsync.model.User;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String avatar;
    private Long profileId;
    private LocalDateTime createdAt;

    /**
     * Map a User domain entity to the UserResponse DTO.
     */
    public static UserResponse fromUser(User user) {
        if (user == null) {
            return null;
        }

        Long resolvedProfileId = null;
        if (user.getRole() == Role.STUDENT && user.getStudentProfile() != null) {
            resolvedName = user.getStudentProfile().getName();
            resolvedProfileId = user.getStudentProfile().getId();
        } else if (user.getRole() == Role.MENTOR && user.getMentorProfile() != null) {
            resolvedName = user.getMentorProfile().getName();
            resolvedProfileId = user.getMentorProfile().getId();
        } else {
            resolvedName = "Admin User";
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(resolvedName)
                .email(user.getEmail())
                .role(user.getRole())
                .profileId(resolvedProfileId)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
