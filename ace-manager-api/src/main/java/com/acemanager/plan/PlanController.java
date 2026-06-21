package com.acemanager.plan;

import com.acemanager.plan.dto.*;
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
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping("/api/v1/plans")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Page<PlanResponse>> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(planService.findAll(pageable));
    }

    @GetMapping("/api/v1/plans/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PlanResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.findById(id));
    }

    @PostMapping("/api/v1/plans")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PlanResponse> create(@RequestBody @Valid CreatePlanRequest request) {
        PlanResponse response = planService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/api/v1/plans/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PlanResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdatePlanRequest request
    ) {
        return ResponseEntity.ok(planService.update(id, request));
    }

    @DeleteMapping("/api/v1/plans/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        planService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/students/{studentId}/plans")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<StudentPlanResponse>> findEnrollments(@PathVariable UUID studentId) {
        return ResponseEntity.ok(planService.findEnrollmentsByStudent(studentId));
    }

    @PostMapping("/api/v1/students/{studentId}/plans")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StudentPlanResponse> enroll(
            @PathVariable UUID studentId,
            @RequestBody @Valid EnrollStudentRequest request
    ) {
        StudentPlanResponse response = planService.enroll(studentId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/students/{studentId}/plans/{id}")
                .buildAndExpand(studentId, response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/api/v1/students/{studentId}/plans/{enrollmentId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StudentPlanResponse> updateEnrollment(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId,
            @RequestBody @Valid UpdateEnrollmentRequest request
    ) {
        return ResponseEntity.ok(planService.updateEnrollment(studentId, enrollmentId, request));
    }

    @DeleteMapping("/api/v1/students/{studentId}/plans/{enrollmentId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> cancelEnrollment(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId
    ) {
        planService.cancelEnrollment(studentId, enrollmentId);
        return ResponseEntity.noContent().build();
    }
}
