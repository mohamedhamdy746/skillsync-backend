package com.pentastack.skillsync.stack;

import com.pentastack.skillsync.stack.dto.StackRequest;
import com.pentastack.skillsync.stack.dto.StackResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stacks")
public class StackController {

    private final StackService stackService;

    public StackController(StackService stackService) {
        this.stackService = stackService;
    }

    @GetMapping
    public List<StackResponse> list() {
        return stackService.listStacks();
    }

    @GetMapping("/{id}")
    public StackResponse get(@PathVariable Long id) {
        return stackService.getStack(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StackResponse> create(@Valid @RequestBody StackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stackService.createStack(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StackResponse update(@PathVariable Long id, @Valid @RequestBody StackRequest request) {
        return stackService.updateStack(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stackService.deleteStack(id);
        return ResponseEntity.noContent().build();
    }
}
