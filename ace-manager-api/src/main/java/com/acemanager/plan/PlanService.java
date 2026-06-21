package com.acemanager.plan;

import com.acemanager.plan.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PlanService {

    Page<PlanResponse> findAll(Pageable pageable);

    PlanResponse findById(UUID id);

    PlanResponse create(CreatePlanRequest request);

    PlanResponse update(UUID id, UpdatePlanRequest request);

    void deactivate(UUID id);

    List<StudentPlanResponse> findEnrollmentsByStudent(UUID studentId);

    StudentPlanResponse enroll(UUID studentId, EnrollStudentRequest request);

    StudentPlanResponse updateEnrollment(UUID studentId, UUID enrollmentId, UpdateEnrollmentRequest request);

    void cancelEnrollment(UUID studentId, UUID enrollmentId);
}
