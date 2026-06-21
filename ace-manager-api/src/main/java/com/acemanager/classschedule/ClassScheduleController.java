package com.acemanager.classschedule;

import com.acemanager.classschedule.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClassScheduleController {

    private final ClassScheduleService classScheduleService;

    // ---- SCHEDULES ----

    @GetMapping("/api/v1/class-schedules")
    @PreAuthorize("hasAnyRole('OWNER','TEACHER')")
    public ResponseEntity<Page<ClassScheduleSummaryResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(classScheduleService.findAll(pageable));
    }

    @GetMapping("/api/v1/class-schedules/{id}")
    @PreAuthorize("hasAnyRole('OWNER','TEACHER')")
    public ResponseEntity<ClassScheduleDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(classScheduleService.findById(id));
    }

    @PostMapping("/api/v1/class-schedules")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ClassScheduleSummaryResponse> create(@Valid @RequestBody CreateClassScheduleRequest request) {
        ClassScheduleSummaryResponse created = classScheduleService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/class-schedules/" + created.id())).body(created);
    }

    @PutMapping("/api/v1/class-schedules/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ClassScheduleDetailResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClassScheduleRequest request) {
        return ResponseEntity.ok(classScheduleService.update(id, request));
    }

    @DeleteMapping("/api/v1/class-schedules/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        classScheduleService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/class-schedules/{id}/students")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ClassScheduleDetailResponse> addStudent(
            @PathVariable UUID id,
            @Valid @RequestBody AddStudentToScheduleRequest request) {
        return ResponseEntity.ok(classScheduleService.addStudent(id, request));
    }

    @DeleteMapping("/api/v1/class-schedules/{id}/students/{studentId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ClassScheduleDetailResponse> removeStudent(
            @PathVariable UUID id,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(classScheduleService.removeStudent(id, studentId));
    }

    // ---- OCCURRENCES ----

    @GetMapping("/api/v1/class-occurrences")
    @PreAuthorize("hasAnyRole('OWNER','TEACHER')")
    public ResponseEntity<List<ClassOccurrenceSummaryResponse>> findOccurrences(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID teacherId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) OccurrenceStatus status) {
        return ResponseEntity.ok(classScheduleService.findOccurrences(dateFrom, dateTo, teacherId, studentId, status));
    }

    @GetMapping("/api/v1/class-occurrences/{id}")
    @PreAuthorize("hasAnyRole('OWNER','TEACHER')")
    public ResponseEntity<ClassOccurrenceDetailResponse> findOccurrenceById(@PathVariable UUID id) {
        return ResponseEntity.ok(classScheduleService.findOccurrenceById(id));
    }

    @PostMapping("/api/v1/class-occurrences/generate")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<GenerateOccurrencesResponse> generateOccurrences(
            @Valid @RequestBody GenerateOccurrencesRequest request) {
        return ResponseEntity.ok(classScheduleService.generateOccurrences(request));
    }

    @PatchMapping("/api/v1/class-occurrences/{id}/teacher")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ClassOccurrenceDetailResponse> substituteTeacher(
            @PathVariable UUID id,
            @Valid @RequestBody SubstituteTeacherRequest request) {
        return ResponseEntity.ok(classScheduleService.substituteTeacher(id, request));
    }

    @PatchMapping("/api/v1/class-occurrences/{id}/cancel")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ClassOccurrenceDetailResponse> cancelOccurrence(@PathVariable UUID id) {
        return ResponseEntity.ok(classScheduleService.cancelOccurrence(id));
    }

    @PutMapping("/api/v1/class-occurrences/{id}/attendance")
    @PreAuthorize("hasAnyRole('OWNER','TEACHER')")
    public ResponseEntity<ClassOccurrenceDetailResponse> updateAttendance(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAttendanceRequest request) {
        return ResponseEntity.ok(classScheduleService.updateAttendance(id, request));
    }
}
