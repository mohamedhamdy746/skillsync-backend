package com.pentastack.skillsync.mentor;

import com.pentastack.skillsync.common.dto.PagedResponse;
import com.pentastack.skillsync.mentor.dto.MentorDetailResponse;
import com.pentastack.skillsync.mentor.dto.MentorListItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.pentastack.skillsync.mentor.dto.MentorProfileUpdateRequest;

@RestController
@RequestMapping("/api/mentors")
public class MentorController {

    private final MentorService mentorService;

    public MentorController(MentorService mentorService) {
        this.mentorService = mentorService;
    }

    @GetMapping
    public PagedResponse<MentorListItemResponse> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String stack,
        @RequestParam(name = "sort_by", required = false) String sortBy,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return mentorService.listMentors(keyword, stack, sortBy, page, size);
    }

    @GetMapping("/{id}")
    public MentorDetailResponse get(@PathVariable Long id) {
        return mentorService.getMentor(id);
    }

    @PutMapping("/{id}")
    public MentorDetailResponse update(@PathVariable Long id, @RequestBody MentorProfileUpdateRequest request) {
        return mentorService.updateMentorProfile(id, request);
    }
}
