package com.pentastack.skillsync.mentor;

import com.pentastack.skillsync.domain.MentorProfile;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class MentorSpecification {

    private MentorSpecification() {}

    public static Specification<MentorProfile> withFilters(String keyword, List<Long> stackIds) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
                if (MentorProfile.class.equals(query.getResultType())) {
                    root.fetch("user", JoinType.LEFT);
                    root.fetch("stack", JoinType.LEFT);
                }
            }

            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                var stackJoin = root.join("stack", JoinType.LEFT);
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("displayName")), pattern),
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("bio")), pattern),
                    cb.like(cb.lower(stackJoin.get("name")), pattern)
                ));
            }

            if (stackIds != null && !stackIds.isEmpty()) {
                predicates.add(root.get("stack").get("id").in(stackIds));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
