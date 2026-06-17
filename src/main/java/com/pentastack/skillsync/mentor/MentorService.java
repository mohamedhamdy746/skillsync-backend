package com.pentastack.skillsync.mentor;

import com.pentastack.skillsync.common.dto.PagedResponse;
import com.pentastack.skillsync.domain.MentorProfile;
import com.pentastack.skillsync.domain.Stack;
import com.pentastack.skillsync.domain.repository.MentorProfileRepository;
import com.pentastack.skillsync.domain.repository.ReviewSessionRepository;
import com.pentastack.skillsync.exception.ApiException;
import com.pentastack.skillsync.mentor.dto.MentorDetailResponse;
import com.pentastack.skillsync.mentor.dto.MentorListItemResponse;
import com.pentastack.skillsync.mentor.dto.MentorProfileUpdateRequest;
import com.pentastack.skillsync.stack.dto.StackResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MentorService {

    private final MentorProfileRepository mentorProfileRepository;
    private final ReviewSessionRepository reviewSessionRepository;

    public MentorService(
        MentorProfileRepository mentorProfileRepository,
        ReviewSessionRepository reviewSessionRepository
    ) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.reviewSessionRepository = reviewSessionRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<MentorListItemResponse> listMentors(
        String keyword,
        String stackParam,
        String sortBy,
        int page,
        int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        List<Long> stackIds = parseStackIds(stackParam);
        Sort sort = MentorSortMapper.resolve(sortBy);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);
        Specification<MentorProfile> spec = MentorSpecification.withFilters(keyword, stackIds);

        Page<MentorProfile> result = mentorProfileRepository.findAll(spec, pageable);
        List<MentorListItemResponse> items = result.getContent().stream()
            .map(this::toListItem)
            .toList();

        return new PagedResponse<>(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public MentorDetailResponse getMentor(Long id) {
        MentorProfile mentor = mentorProfileRepository.findWithUserById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor not found"));
        return toDetail(mentor);
    }

    @Transactional
    public MentorDetailResponse updateMentorProfile(Long id, MentorProfileUpdateRequest request) {
        MentorProfile mentor = mentorProfileRepository.findWithUserById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor not found"));
        
        mentor.updateProfile(
            request.title(),
            request.bio(),
            request.hourlyRate(),
            request.available()
        );
        
        return toDetail(mentor);
    }

    private List<Long> parseStackIds(String stackParam) {
        if (stackParam == null || stackParam.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(stackParam.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(Long::valueOf)
            .toList();
    }

    private MentorListItemResponse toListItem(MentorProfile mentor) {
        return new MentorListItemResponse(
            mentor.getId(),
            mentor.getDisplayName(),
            mentor.getUser().getEmail(),
            mentor.getTitle(),
            mentor.getBio(),
            mentor.getRating(),
            mentor.getHourlyRate(),
            mentor.isAvailable(),
            toStackResponses(mentor.getStack()),
            reviewSessionRepository.countByMentor_Id(mentor.getId()),
            true
        );
    }

    private MentorDetailResponse toDetail(MentorProfile mentor) {
        return new MentorDetailResponse(
            mentor.getId(),
            mentor.getDisplayName(),
            mentor.getUser().getEmail(),
            mentor.getTitle(),
            mentor.getBio(),
            mentor.getRating(),
            mentor.getHourlyRate(),
            mentor.isAvailable(),
            toStackResponses(mentor.getStack()),
            reviewSessionRepository.countByMentor_Id(mentor.getId()),
            true
        );
    }

    private List<StackResponse> toStackResponses(Stack stack) {
        if (stack == null) {
            return List.of();
        }
        return List.of(new StackResponse(stack.getId(), stack.getName(), stack.getDescription()));
    }
}
