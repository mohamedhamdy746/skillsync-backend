package com.pentastack.skillsync.stack;

import com.pentastack.skillsync.domain.Stack;
import com.pentastack.skillsync.domain.repository.StackRepository;
import com.pentastack.skillsync.exception.ApiException;
import com.pentastack.skillsync.stack.dto.StackRequest;
import com.pentastack.skillsync.stack.dto.StackResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StackService {

    private final StackRepository stackRepository;

    public StackService(StackRepository stackRepository) {
        this.stackRepository = stackRepository;
    }

    @Transactional(readOnly = true)
    public List<StackResponse> listStacks() {
        return stackRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StackResponse getStack(Long id) {
        return toResponse(findStack(id));
    }

    @Transactional
    public StackResponse createStack(StackRequest request) {
        validateUniqueName(request.name(), null);
        Stack stack = stackRepository.save(new Stack(request.name().trim(), request.description()));
        return toResponse(stack);
    }

    @Transactional
    public StackResponse updateStack(Long id, StackRequest request) {
        Stack stack = findStack(id);
        validateUniqueName(request.name(), id);
        stack.setName(request.name().trim());
        stack.setDescription(request.description());
        return toResponse(stack);
    }

    @Transactional
    public void deleteStack(Long id) {
        if (!stackRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Stack not found");
        }
        stackRepository.deleteById(id);
    }

    private Stack findStack(Long id) {
        return stackRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Stack not found"));
    }

    private void validateUniqueName(String name, Long excludeId) {
        boolean exists = excludeId == null
            ? stackRepository.existsByNameIgnoreCase(name.trim())
            : stackRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), excludeId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "A stack with this name already exists");
        }
    }

    private StackResponse toResponse(Stack stack) {
        return new StackResponse(stack.getId(), stack.getName(), stack.getDescription());
    }
}
