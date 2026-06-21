package com.acemanager.teacher;

import com.acemanager.shared.exception.BusinessException;
import com.acemanager.shared.exception.ResourceNotFoundException;
import com.acemanager.student.Student;
import com.acemanager.student.StudentRepository;
import com.acemanager.teacher.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherStudentConfigRepository configRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherSummaryResponse> findAll(Pageable pageable) {
        return teacherRepository.findAllActive(pageable).map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDetailResponse findById(UUID id) {
        Teacher teacher = loadActive(id);
        List<TeacherStudentConfig> configs = configRepository.findAllByTeacherId(id);
        return mapToDetailResponse(teacher, configs);
    }

    @Override
    public TeacherDetailResponse create(CreateTeacherRequest request) {
        validatePayoutFields(request.payoutModel(), request.defaultPercentage(), request.defaultHourlyRate());

        Teacher teacher = Teacher.builder()
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .payoutModel(request.payoutModel())
                .defaultPercentage(request.defaultPercentage())
                .defaultHourlyRate(request.defaultHourlyRate())
                .status(TeacherStatus.ACTIVE)
                .build();

        teacher = teacherRepository.save(teacher);
        return mapToDetailResponse(teacher, List.of());
    }

    @Override
    public TeacherDetailResponse update(UUID id, UpdateTeacherRequest request) {
        Teacher teacher = loadActive(id);
        validatePayoutFields(request.payoutModel(), request.defaultPercentage(), request.defaultHourlyRate());

        teacher.setName(request.name());
        teacher.setPhone(request.phone());
        teacher.setEmail(request.email());
        teacher.setPayoutModel(request.payoutModel());
        teacher.setDefaultPercentage(request.defaultPercentage());
        teacher.setDefaultHourlyRate(request.defaultHourlyRate());
        teacher.setStatus(request.status());

        List<TeacherStudentConfig> configs = configRepository.findAllByTeacherId(id);
        return mapToDetailResponse(teacherRepository.save(teacher), configs);
    }

    @Override
    public void delete(UUID id) {
        Teacher teacher = loadActive(id);
        teacher.setDeletedAt(LocalDateTime.now());
        teacherRepository.save(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentPayoutConfigResponse> findStudentConfigs(UUID teacherId) {
        Teacher teacher = loadActive(teacherId);
        return configRepository.findAllByTeacherId(teacherId)
                .stream()
                .map(c -> mapToConfigResponse(c, teacher))
                .toList();
    }

    @Override
    public StudentPayoutConfigResponse upsertStudentConfig(UUID teacherId, UUID studentId, UpsertStudentConfigRequest request) {
        Teacher teacher = loadActive(teacherId);
        Student student = studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        TeacherStudentConfig config = configRepository
                .findByTeacherIdAndStudentId(teacherId, studentId)
                .orElseGet(() -> TeacherStudentConfig.builder()
                        .teacher(teacher)
                        .student(student)
                        .build());

        config.setOverridePercentage(request.overridePercentage());
        config.setOverrideHourlyRate(request.overrideHourlyRate());

        return mapToConfigResponse(configRepository.save(config), teacher);
    }

    @Override
    public void removeStudentConfig(UUID teacherId, UUID studentId) {
        loadActive(teacherId);
        TeacherStudentConfig config = configRepository
                .findByTeacherIdAndStudentId(teacherId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student config not found for teacher " + teacherId + " and student " + studentId));
        configRepository.delete(config);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveEffectivePayout(UUID teacherId, UUID studentId, BigDecimal studentValue, int durationMinutes) {
        Teacher teacher = loadActive(teacherId);

        return configRepository.findByTeacherIdAndStudentId(teacherId, studentId)
                .map(config -> computePayout(teacher, config, studentValue, durationMinutes))
                .orElseGet(() -> computeDefaultPayout(teacher, studentValue, durationMinutes));
    }

    private BigDecimal computePayout(Teacher teacher, TeacherStudentConfig config,
                                     BigDecimal studentValue, int durationMinutes) {
        if (teacher.getPayoutModel() == PayoutModel.PERCENTAGE) {
            BigDecimal pct = config.getOverridePercentage() != null
                    ? config.getOverridePercentage()
                    : teacher.getDefaultPercentage();
            return studentValue.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal rate = config.getOverrideHourlyRate() != null
                    ? config.getOverrideHourlyRate()
                    : teacher.getDefaultHourlyRate();
            return rate.multiply(BigDecimal.valueOf(durationMinutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal computeDefaultPayout(Teacher teacher, BigDecimal studentValue, int durationMinutes) {
        if (teacher.getPayoutModel() == PayoutModel.PERCENTAGE) {
            return studentValue.multiply(teacher.getDefaultPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            return teacher.getDefaultHourlyRate()
                    .multiply(BigDecimal.valueOf(durationMinutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
    }

    private void validatePayoutFields(PayoutModel model, BigDecimal percentage, BigDecimal hourlyRate) {
        if (model == PayoutModel.PERCENTAGE) {
            if (percentage == null) {
                throw new BusinessException("defaultPercentage is required when payoutModel is PERCENTAGE");
            }
            if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("defaultPercentage must be between 0 and 100");
            }
        }
        if (model == PayoutModel.HOURLY_RATE) {
            if (hourlyRate == null) {
                throw new BusinessException("defaultHourlyRate is required when payoutModel is HOURLY_RATE");
            }
            if (hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("defaultHourlyRate must be greater than 0");
            }
        }
    }

    private Teacher loadActive(UUID id) {
        return teacherRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
    }

    private TeacherSummaryResponse mapToSummaryResponse(Teacher teacher) {
        long count = teacherRepository.countStudentConfigs(teacher.getId());
        return new TeacherSummaryResponse(
                teacher.getId(),
                teacher.getName(),
                teacher.getPhone(),
                teacher.getPayoutModel(),
                teacher.getDefaultPercentage(),
                teacher.getDefaultHourlyRate(),
                count,
                teacher.getStatus()
        );
    }

    private TeacherDetailResponse mapToDetailResponse(Teacher teacher, List<TeacherStudentConfig> configs) {
        List<StudentPayoutConfigResponse> configResponses = configs.stream()
                .map(c -> mapToConfigResponse(c, teacher))
                .toList();

        return new TeacherDetailResponse(
                teacher.getId(),
                teacher.getName(),
                teacher.getPhone(),
                teacher.getEmail(),
                teacher.getPayoutModel(),
                teacher.getDefaultPercentage(),
                teacher.getDefaultHourlyRate(),
                teacher.getStatus(),
                configResponses
        );
    }

    private StudentPayoutConfigResponse mapToConfigResponse(TeacherStudentConfig config, Teacher teacher) {
        boolean isOverride = config.getOverridePercentage() != null || config.getOverrideHourlyRate() != null;

        BigDecimal effectivePct = config.getOverridePercentage() != null
                ? config.getOverridePercentage()
                : teacher.getDefaultPercentage();

        BigDecimal effectiveRate = config.getOverrideHourlyRate() != null
                ? config.getOverrideHourlyRate()
                : teacher.getDefaultHourlyRate();

        return new StudentPayoutConfigResponse(
                config.getStudent().getId(),
                config.getStudent().getName(),
                effectivePct,
                effectiveRate,
                isOverride
        );
    }
}
