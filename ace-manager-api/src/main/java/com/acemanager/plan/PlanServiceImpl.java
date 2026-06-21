package com.acemanager.plan;

import com.acemanager.plan.dto.*;
import com.acemanager.shared.exception.BusinessException;
import com.acemanager.shared.exception.ResourceNotFoundException;
import com.acemanager.student.Student;
import com.acemanager.student.StudentRepository;
import com.acemanager.teacher.Teacher;
import com.acemanager.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PlanResponse> findAll(Pageable pageable) {
        return planRepository.findAllActive(pageable).map(this::mapToPlanResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse findById(UUID id) {
        return mapToPlanResponse(loadActivePlan(id));
    }

    @Override
    public PlanResponse create(CreatePlanRequest request) {
        validatePlanFields(request.type(), request.billingDayOfMonth(),
                request.totalClasses(), request.maxStudents());

        Plan plan = Plan.builder()
                .name(request.name())
                .description(request.description())
                .type(request.type())
                .referencePrice(request.referencePrice())
                .weeklyClassCount(request.weeklyClassCount())
                .billingDayOfMonth(request.billingDayOfMonth())
                .totalClasses(request.totalClasses())
                .maxStudents(request.maxStudents())
                .status(PlanStatus.ACTIVE)
                .build();

        return mapToPlanResponse(planRepository.save(plan));
    }

    @Override
    public PlanResponse update(UUID id, UpdatePlanRequest request) {
        Plan plan = loadActivePlan(id);
        validatePlanFields(request.type(), request.billingDayOfMonth(),
                request.totalClasses(), request.maxStudents());

        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setType(request.type());
        plan.setReferencePrice(request.referencePrice());
        plan.setWeeklyClassCount(request.weeklyClassCount());
        plan.setBillingDayOfMonth(request.billingDayOfMonth());
        plan.setTotalClasses(request.totalClasses());
        plan.setMaxStudents(request.maxStudents());
        plan.setStatus(request.status());

        return mapToPlanResponse(planRepository.save(plan));
    }

    @Override
    public void deactivate(UUID id) {
        Plan plan = loadActivePlan(id);
        plan.setStatus(PlanStatus.INACTIVE);
        plan.setDeletedAt(LocalDateTime.now());
        planRepository.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentPlanResponse> findEnrollmentsByStudent(UUID studentId) {
        loadActiveStudent(studentId);
        return studentPlanRepository.findActiveByStudentId(studentId)
                .stream()
                .map(this::mapToStudentPlanResponse)
                .toList();
    }

    @Override
    public StudentPlanResponse enroll(UUID studentId, EnrollStudentRequest request) {
        Student student = loadActiveStudent(studentId);
        Plan plan = loadActivePlan(request.planId());

        if (request.billedValue().compareTo(plan.getReferencePrice()) > 0) {
            throw new BusinessException("Billed value cannot exceed the plan's reference price");
        }

        if (plan.getType() == PlanType.GROUP && plan.getMaxStudents() != null) {
            long active = studentPlanRepository.countActiveByPlanId(plan.getId());
            if (active >= plan.getMaxStudents()) {
                throw new BusinessException("Group plan has reached maximum capacity of " + plan.getMaxStudents() + " students");
            }
        }

        if (plan.getType() == PlanType.MONTHLY && request.startDate() != null
                && plan.getBillingDayOfMonth() != null) {
            int day = plan.getBillingDayOfMonth();
            if (day < 1 || day > 28) {
                throw new BusinessException("Billing day must be between 1 and 28");
            }
        }

        Teacher teacher = request.teacherId() != null
                ? teacherRepository.findActiveById(request.teacherId())
                        .orElseThrow(() -> new ResourceNotFoundException("Teacher", request.teacherId()))
                : null;

        Integer remainingClasses = plan.getType() == PlanType.BUNDLE ? plan.getTotalClasses() : null;

        StudentPlan enrollment = StudentPlan.builder()
                .student(student)
                .plan(plan)
                .teacher(teacher)
                .billedValue(request.billedValue())
                .startDate(request.startDate())
                .remainingClasses(remainingClasses)
                .status(StudentPlanStatus.ACTIVE)
                .build();

        return mapToStudentPlanResponse(studentPlanRepository.save(enrollment));
    }

    @Override
    public StudentPlanResponse updateEnrollment(UUID studentId, UUID enrollmentId, UpdateEnrollmentRequest request) {
        loadActiveStudent(studentId);
        StudentPlan enrollment = studentPlanRepository.findByIdAndStudentId(enrollmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));

        if (request.billedValue() != null) {
            if (request.billedValue().compareTo(enrollment.getPlan().getReferencePrice()) > 0) {
                throw new BusinessException("Billed value cannot exceed the plan's reference price");
            }
            enrollment.setBilledValue(request.billedValue());
        }

        if (request.status() != null) {
            enrollment.setStatus(request.status());
        }

        return mapToStudentPlanResponse(studentPlanRepository.save(enrollment));
    }

    @Override
    public void cancelEnrollment(UUID studentId, UUID enrollmentId) {
        loadActiveStudent(studentId);
        StudentPlan enrollment = studentPlanRepository.findByIdAndStudentId(enrollmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));
        enrollment.setStatus(StudentPlanStatus.CANCELLED);
        studentPlanRepository.save(enrollment);
    }

    private void validatePlanFields(PlanType type, Integer billingDay, Integer totalClasses, Integer maxStudents) {
        if (type == PlanType.MONTHLY && billingDay == null) {
            throw new BusinessException("billingDayOfMonth is required for MONTHLY plans");
        }
        if (type == PlanType.BUNDLE && totalClasses == null) {
            throw new BusinessException("totalClasses is required for BUNDLE plans");
        }
        if (type == PlanType.GROUP && maxStudents == null) {
            throw new BusinessException("maxStudents is required for GROUP plans");
        }
        if (billingDay != null && (billingDay < 1 || billingDay > 28)) {
            throw new BusinessException("billingDayOfMonth must be between 1 and 28");
        }
    }

    private Plan loadActivePlan(UUID id) {
        return planRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", id));
    }

    private Student loadActiveStudent(UUID id) {
        return studentRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    private PlanResponse mapToPlanResponse(Plan plan) {
        long enrollments = planRepository.countActiveEnrollments(plan.getId());
        return new PlanResponse(
                plan.getId(), plan.getName(), plan.getDescription(),
                plan.getType(), plan.getReferencePrice(),
                plan.getWeeklyClassCount(), plan.getBillingDayOfMonth(),
                plan.getTotalClasses(), plan.getMaxStudents(),
                enrollments, plan.getStatus()
        );
    }

    private StudentPlanResponse mapToStudentPlanResponse(StudentPlan sp) {
        Plan plan = sp.getPlan();
        BigDecimal discount = plan.getReferencePrice().subtract(sp.getBilledValue());
        boolean lowAlert = sp.getRemainingClasses() != null && sp.getRemainingClasses() <= 2;

        return new StudentPlanResponse(
                sp.getId(), plan.getId(), plan.getName(), plan.getType(),
                sp.getTeacher() != null ? sp.getTeacher().getId() : null,
                sp.getTeacher() != null ? sp.getTeacher().getName() : null,
                plan.getReferencePrice(), sp.getBilledValue(), discount,
                sp.getStartDate(), sp.getEndDate(),
                sp.getRemainingClasses(), lowAlert, sp.getStatus()
        );
    }
}
