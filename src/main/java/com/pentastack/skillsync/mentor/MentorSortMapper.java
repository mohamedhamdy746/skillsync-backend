package com.pentastack.skillsync.mentor;

import java.util.Locale;
import org.springframework.data.domain.Sort;

public final class MentorSortMapper {

    private MentorSortMapper() {}

    public static Sort resolve(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Order.desc("available"), Sort.Order.desc("rating"));
        }

        return switch (sortBy.toLowerCase(Locale.ROOT)) {
            case "rating" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("available"));
            case "price" -> Sort.by(Sort.Order.asc("hourlyRate"), Sort.Order.desc("available"));
            case "availability" -> Sort.by(Sort.Order.desc("available"), Sort.Order.desc("rating"));
            default -> Sort.by(Sort.Order.desc("available"), Sort.Order.desc("rating"));
        };
    }
}
