package com.acemanager.teacher;

import com.acemanager.teacher.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Page<TeacherSummaryResponse>> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(teacherService.findAll(pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> getOwnProfile() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TeacherDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(teacherService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TeacherDetailResponse> create(@RequestBody @Valid CreateTeacherRequest request) {
        TeacherDetailResponse response = teacherService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TeacherDetailResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateTeacherRequest request
    ) {
        return ResponseEntity.ok(teacherService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/student-configs")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<StudentPayoutConfigResponse>> findStudentConfigs(@PathVariable UUID id) {
        return ResponseEntity.ok(teacherService.findStudentConfigs(id));
    }

    @PutMapping("/{id}/student-configs/{studentId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StudentPayoutConfigResponse> upsertStudentConfig(
            @PathVariable UUID id,
            @PathVariable UUID studentId,
            @RequestBody @Valid UpsertStudentConfigRequest request
    ) {
        return ResponseEntity.ok(teacherService.upsertStudentConfig(id, studentId, request));
    }

    @DeleteMapping("/{id}/student-configs/{studentId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> removeStudentConfig(
            @PathVariable UUID id,
            @PathVariable UUID studentId
    ) {
        teacherService.removeStudentConfig(id, studentId);
        return ResponseEntity.noContent().build();
    }
}
