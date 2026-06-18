package com.acemanager.student;

import com.acemanager.shared.exception.BusinessException;
import com.acemanager.shared.exception.ResourceNotFoundException;
import com.acemanager.student.dto.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<StudentSummaryResponse> findAll(StudentFilterParams filters, Pageable pageable) {
        Specification<Student> spec = buildSpecification(filters);
        return studentRepository.findAll(spec, pageable).map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDetailResponse findById(UUID id) {
        Student student = studentRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        return mapToDetailResponse(student);
    }

    @Override
    public StudentDetailResponse create(CreateStudentRequest request) {
        validateBillingValues(request.agreedMonthlyValue(), request.currentMonthlyValue());
        validateGuardianRequirements(request.birthDate(), request.guardianName(), request.guardianPhone());

        Student student = Student.builder()
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .birthDate(request.birthDate())
                .guardianName(request.guardianName())
                .guardianPhone(request.guardianPhone())
                .agreedMonthlyValue(request.agreedMonthlyValue())
                .currentMonthlyValue(request.currentMonthlyValue())
                .preferredPaymentMethod(request.preferredPaymentMethod())
                .status(StudentStatus.ACTIVE)
                .notes(request.notes())
                .build();

        return mapToDetailResponse(studentRepository.save(student));
    }

    @Override
    public StudentDetailResponse update(UUID id, UpdateStudentRequest request) {
        Student student = studentRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));

        validateBillingValues(request.agreedMonthlyValue(), request.currentMonthlyValue());
        validateGuardianRequirements(request.birthDate(), request.guardianName(), request.guardianPhone());

        student.setName(request.name());
        student.setPhone(request.phone());
        student.setEmail(request.email());
        student.setBirthDate(request.birthDate());
        student.setGuardianName(request.guardianName());
        student.setGuardianPhone(request.guardianPhone());
        student.setAgreedMonthlyValue(request.agreedMonthlyValue());
        student.setCurrentMonthlyValue(request.currentMonthlyValue());
        student.setPreferredPaymentMethod(request.preferredPaymentMethod());
        student.setStatus(request.status());
        student.setNotes(request.notes());

        return mapToDetailResponse(studentRepository.save(student));
    }

    @Override
    public void delete(UUID id) {
        Student student = studentRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        student.setDeletedAt(LocalDateTime.now());
        studentRepository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDetailResponse findCurrentStudentProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentRepository.findActiveByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for current user"));
        return mapToDetailResponse(student);
    }

    private Specification<Student> buildSpecification(StudentFilterParams filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filters.status() != null) {
                predicates.add(cb.equal(root.get("status"), filters.status()));
            }

            if (filters.search() != null && !filters.search().isBlank()) {
                String pattern = "%" + filters.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateBillingValues(BigDecimal agreed, BigDecimal current) {
        if (agreed != null && current != null && current.compareTo(agreed) > 0) {
            throw new BusinessException("Current monthly value cannot exceed agreed monthly value");
        }
    }

    private void validateGuardianRequirements(LocalDate birthDate, String guardianName, String guardianPhone) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now().minusYears(18))) {
            if (guardianName == null || guardianName.isBlank()) {
                throw new BusinessException("Guardian name is required for students under 18");
            }
            if (guardianPhone == null || guardianPhone.isBlank()) {
                throw new BusinessException("Guardian phone is required for students under 18");
            }
        }
    }

    private StudentSummaryResponse mapToSummaryResponse(Student student) {
        return new StudentSummaryResponse(
                student.getId(),
                student.getName(),
                student.getPhone(),
                student.getCurrentMonthlyValue(),
                student.getPreferredPaymentMethod(),
                student.getStatus(),
                null
        );
    }

    private StudentDetailResponse mapToDetailResponse(Student student) {
        BigDecimal agreed = student.getAgreedMonthlyValue();
        BigDecimal current = student.getCurrentMonthlyValue();

        BigDecimal discountAmount = (agreed != null && current != null)
                ? agreed.subtract(current)
                : BigDecimal.ZERO;

        BigDecimal discountPercentage = (agreed != null && current != null
                && agreed.compareTo(BigDecimal.ZERO) > 0)
                ? BigDecimal.ONE
                        .subtract(current.divide(agreed, 4, RoundingMode.HALF_UP))
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new StudentDetailResponse(
                student.getId(),
                student.getName(),
                student.getPhone(),
                student.getEmail(),
                student.getBirthDate(),
                student.getGuardianName(),
                student.getGuardianPhone(),
                agreed,
                current,
                discountAmount,
                discountPercentage,
                student.getPreferredPaymentMethod(),
                student.getStatus(),
                student.getNotes(),
                student.getCreatedAt()
        );
    }
}
