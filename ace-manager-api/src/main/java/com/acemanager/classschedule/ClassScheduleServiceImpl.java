package com.acemanager.classschedule;

import com.acemanager.classschedule.dto.*;
import com.acemanager.plan.StudentPlan;
import com.acemanager.plan.StudentPlanRepository;
import com.acemanager.shared.exception.BusinessException;
import com.acemanager.shared.exception.ResourceNotFoundException;
import com.acemanager.student.Student;
import com.acemanager.student.StudentRepository;
import com.acemanager.teacher.PayoutModel;
import com.acemanager.teacher.Teacher;
import com.acemanager.teacher.TeacherRepository;
import com.acemanager.teacher.TeacherStudentConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ClassScheduleServiceImpl implements ClassScheduleService {

    private final ClassScheduleRepository scheduleRepo;
    private final ClassScheduleStudentRepository scheduleStudentRepo;
    private final ClassOccurrenceRepository occurrenceRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final StudentRepository studentRepo;
    private final StudentPlanRepository studentPlanRepo;
    private final TeacherStudentConfigRepository teacherStudentConfigRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<ClassScheduleSummaryResponse> findAll(Pageable pageable) {
        return scheduleRepo.findAllActive(pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleDetailResponse findById(UUID id) {
        return toDetail(findActiveSchedule(id));
    }

    @Override
    public ClassScheduleSummaryResponse create(CreateClassScheduleRequest req) {
        Teacher teacher = findTeacher(req.teacherId());
        int maxStudents = resolveMaxStudents(req.type(), req.maxStudents());

        ClassSchedule schedule = ClassSchedule.builder()
                .name(req.name())
                .dayOfWeek(req.dayOfWeek())
                .startTime(req.startTime())
                .durationMinutes(req.durationMinutes())
                .teacher(teacher)
                .type(req.type())
                .maxStudents(maxStudents)
                .status(ClassScheduleStatus.ACTIVE)
                .build();

        return toSummary(scheduleRepo.save(schedule));
    }

    @Override
    public ClassScheduleDetailResponse update(UUID id, UpdateClassScheduleRequest req) {
        ClassSchedule schedule = findActiveSchedule(id);
        Teacher teacher = findTeacher(req.teacherId());
        int maxStudents = resolveMaxStudents(req.type(), req.maxStudents());

        schedule.setName(req.name());
        schedule.setDayOfWeek(req.dayOfWeek());
        schedule.setStartTime(req.startTime());
        schedule.setDurationMinutes(req.durationMinutes());
        schedule.setTeacher(teacher);
        schedule.setType(req.type());
        schedule.setMaxStudents(maxStudents);
        schedule.setStatus(req.status());

        return toDetail(scheduleRepo.save(schedule));
    }

    @Override
    public void deactivate(UUID id) {
        ClassSchedule schedule = findActiveSchedule(id);
        schedule.setStatus(ClassScheduleStatus.INACTIVE);
        schedule.setDeletedAt(java.time.LocalDateTime.now());
        scheduleRepo.save(schedule);
    }

    @Override
    public ClassScheduleDetailResponse addStudent(UUID scheduleId, AddStudentToScheduleRequest req) {
        ClassSchedule schedule = findActiveSchedule(scheduleId);

        if (scheduleStudentRepo.findByScheduleAndStudent(scheduleId, req.studentId()).isPresent()) {
            throw new BusinessException("Aluno já está inscrito nesta aula");
        }

        long enrolled = scheduleStudentRepo.countEnrolled(scheduleId);
        if (enrolled >= schedule.getMaxStudents()) {
            throw new BusinessException("Aula já atingiu o limite de alunos (" + schedule.getMaxStudents() + ")");
        }

        Student student = studentRepo.findById(req.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + req.studentId()));

        StudentPlan studentPlan = null;
        if (req.studentPlanId() != null) {
            studentPlan = studentPlanRepo.findById(req.studentPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Matrícula no plano não encontrada"));
        }

        ClassScheduleStudent link = ClassScheduleStudent.builder()
                .classSchedule(schedule)
                .student(student)
                .studentPlan(studentPlan)
                .build();

        scheduleStudentRepo.save(link);
        // Add to in-memory collection to avoid L1 cache re-fetch issues
        schedule.getEnrolledStudents().add(link);
        return toDetail(schedule);
    }

    @Override
    public ClassScheduleDetailResponse removeStudent(UUID scheduleId, UUID studentId) {
        ClassSchedule schedule = findActiveSchedule(scheduleId);
        ClassScheduleStudent link = scheduleStudentRepo.findByScheduleAndStudent(scheduleId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não está inscrito nesta aula"));
        link.setDeletedAt(java.time.LocalDateTime.now());
        scheduleStudentRepo.save(link);
        // link already in schedule.enrolledStudents with deletedAt set — toDetail filter excludes it
        return toDetail(schedule);
    }

    @Override
    public GenerateOccurrencesResponse generateOccurrences(GenerateOccurrencesRequest req) {
        List<ClassSchedule> schedules = req.scheduleId() != null
                ? List.of(findActiveSchedule(req.scheduleId()))
                : scheduleRepo.findAllActive(Pageable.unpaged()).getContent();

        int generated = 0;
        int skipped   = 0;

        for (ClassSchedule schedule : schedules) {
            LocalDate cursor = req.fromDate();
            while (!cursor.isAfter(req.toDate())) {
                if (cursor.getDayOfWeek() == schedule.getDayOfWeek()) {
                    if (occurrenceRepo.existsByClassScheduleIdAndOccurrenceDate(schedule.getId(), cursor)) {
                        skipped++;
                    } else {
                        ClassOccurrence occurrence = ClassOccurrence.builder()
                                .classSchedule(schedule)
                                .occurrenceDate(cursor)
                                .teacher(schedule.getTeacher())
                                .status(OccurrenceStatus.SCHEDULED)
                                .build();
                        occurrenceRepo.save(occurrence);
                        generated++;
                    }
                }
                cursor = cursor.plusDays(1);
            }
        }

        return new GenerateOccurrencesResponse(generated, skipped);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassOccurrenceSummaryResponse> findOccurrences(LocalDate dateFrom, LocalDate dateTo,
                                                                  UUID teacherId, UUID studentId,
                                                                  OccurrenceStatus status) {
        List<ClassOccurrence> results = (dateFrom != null && dateTo != null)
                ? occurrenceRepo.findByDateRange(dateFrom, dateTo)
                : occurrenceRepo.findAllActive();

        // Optional filters applied in Java to avoid PostgreSQL null type inference errors with UUID/enum params
        if (teacherId != null) {
            results = results.stream().filter(o -> o.getTeacher().getId().equals(teacherId)).toList();
        }
        if (studentId != null) {
            List<UUID> occurrenceIds = occurrenceRepo.findOccurrenceIdsByStudent(studentId);
            results = results.stream().filter(o -> occurrenceIds.contains(o.getId())).toList();
        }
        if (status != null) {
            results = results.stream().filter(o -> o.getStatus() == status).toList();
        }

        return results.stream().map(this::toOccurrenceSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassOccurrenceDetailResponse findOccurrenceById(UUID id) {
        return toOccurrenceDetail(findOccurrence(id));
    }

    @Override
    public ClassOccurrenceDetailResponse substituteTeacher(UUID occurrenceId, SubstituteTeacherRequest req) {
        ClassOccurrence occurrence = findOccurrence(occurrenceId);
        Teacher teacher = findTeacher(req.teacherId());
        occurrence.setTeacher(teacher);
        return toOccurrenceDetail(occurrenceRepo.save(occurrence));
    }

    @Override
    public ClassOccurrenceDetailResponse cancelOccurrence(UUID occurrenceId) {
        ClassOccurrence occurrence = findOccurrence(occurrenceId);
        occurrence.setStatus(OccurrenceStatus.CANCELLED);
        attendanceRepo.findByOccurrenceId(occurrenceId).forEach(a -> {
            a.setStatus(AttendanceStatus.ABSENT);
            a.setStudentBilledValue(null);
            a.setTeacherPayoutValue(null);
        });
        return toOccurrenceDetail(occurrenceRepo.save(occurrence));
    }

    @Override
    public ClassOccurrenceDetailResponse updateAttendance(UUID occurrenceId, UpdateAttendanceRequest req) {
        ClassOccurrence occurrence = findOccurrence(occurrenceId);
        if (occurrence.getStatus() == OccurrenceStatus.CANCELLED) {
            throw new BusinessException("Não é possível registrar presença em aula cancelada");
        }

        List<AttendanceRecord> savedRecords = new ArrayList<>();

        for (UpdateAttendanceRequest.AttendanceEntry entry : req.attendances()) {
            Student student = studentRepo.findById(entry.studentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + entry.studentId()));

            AttendanceRecord record = attendanceRepo
                    .findByOccurrenceAndStudent(occurrenceId, entry.studentId())
                    .orElseGet(() -> AttendanceRecord.builder()
                            .classOccurrence(occurrence)
                            .student(student)
                            .status(entry.status())
                            .build());

            record.setStatus(entry.status());

            if (entry.status() == AttendanceStatus.PRESENT) {
                BigDecimal billedValue = resolveBilledValue(occurrence, student);
                BigDecimal payoutValue = resolvePayoutValue(occurrence, student, billedValue);
                record.setStudentBilledValue(billedValue);
                record.setTeacherPayoutValue(payoutValue);
                decrementBundleClasses(occurrence, student);
            } else {
                record.setStudentBilledValue(null);
                record.setTeacherPayoutValue(null);
            }

            savedRecords.add(attendanceRepo.save(record));
        }

        occurrence.setStatus(OccurrenceStatus.COMPLETED);
        // Replace in-memory collection to avoid L1 cache re-fetch issues
        occurrence.getAttendanceRecords().clear();
        occurrence.getAttendanceRecords().addAll(savedRecords);
        occurrenceRepo.save(occurrence);

        return toOccurrenceDetail(occurrence);
    }

    // ---- private helpers ----

    private ClassSchedule findActiveSchedule(UUID id) {
        return scheduleRepo.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade de aula não encontrada: " + id));
    }

    private ClassOccurrence findOccurrence(UUID id) {
        return occurrenceRepo.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aula não encontrada: " + id));
    }

    private Teacher findTeacher(UUID id) {
        return teacherRepo.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado: " + id));
    }

    private int resolveMaxStudents(ClassType type, Integer provided) {
        if (type == ClassType.INDIVIDUAL) return 1;
        return (provided != null && provided > 0) ? provided : 1;
    }

    private BigDecimal resolveBilledValue(ClassOccurrence occurrence, Student student) {
        return scheduleStudentRepo
                .findByScheduleAndStudent(occurrence.getClassSchedule().getId(), student.getId())
                .map(link -> link.getStudentPlan() != null
                        ? link.getStudentPlan().getBilledValue()
                        : student.getCurrentMonthlyValue())
                .orElse(student.getCurrentMonthlyValue() != null ? student.getCurrentMonthlyValue() : BigDecimal.ZERO);
    }

    private BigDecimal resolvePayoutValue(ClassOccurrence occurrence, Student student, BigDecimal billedValue) {
        Teacher teacher = occurrence.getTeacher();
        var configOpt = teacherStudentConfigRepo.findByTeacherIdAndStudentId(teacher.getId(), student.getId());

        BigDecimal percentage  = configOpt.map(c -> c.getOverridePercentage() != null
                ? c.getOverridePercentage() : teacher.getDefaultPercentage())
                .orElse(teacher.getDefaultPercentage());
        BigDecimal hourlyRate  = configOpt.map(c -> c.getOverrideHourlyRate() != null
                ? c.getOverrideHourlyRate() : teacher.getDefaultHourlyRate())
                .orElse(teacher.getDefaultHourlyRate());

        if (teacher.getPayoutModel() == PayoutModel.PERCENTAGE && percentage != null && billedValue != null) {
            return billedValue.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if (teacher.getPayoutModel() == PayoutModel.HOURLY_RATE && hourlyRate != null) {
            BigDecimal hours = BigDecimal.valueOf(occurrence.getClassSchedule().getDurationMinutes())
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            return hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private void decrementBundleClasses(ClassOccurrence occurrence, Student student) {
        scheduleStudentRepo.findByScheduleAndStudent(occurrence.getClassSchedule().getId(), student.getId())
                .ifPresent(link -> {
                    if (link.getStudentPlan() != null) {
                        StudentPlan plan = link.getStudentPlan();
                        if (plan.getRemainingClasses() != null && plan.getRemainingClasses() > 0) {
                            plan.setRemainingClasses(plan.getRemainingClasses() - 1);
                            studentPlanRepo.save(plan);
                        }
                    }
                });
    }

    private ClassScheduleSummaryResponse toSummary(ClassSchedule s) {
        int enrolled = (int) s.getEnrolledStudents().stream()
                .filter(e -> e.getDeletedAt() == null).count();
        return new ClassScheduleSummaryResponse(
                s.getId(), s.getName(), s.getDayOfWeek(), s.getStartTime(),
                s.getDurationMinutes(), s.getTeacher().getId(), s.getTeacher().getName(),
                s.getType(), s.getMaxStudents(), enrolled, s.getStatus());
    }

    private ClassScheduleDetailResponse toDetail(ClassSchedule s) {
        List<ScheduleStudentResponse> students = s.getEnrolledStudents().stream()
                .filter(e -> e.getDeletedAt() == null)
                .map(e -> new ScheduleStudentResponse(
                        e.getId(), e.getStudent().getId(), e.getStudent().getName(),
                        e.getStudentPlan() != null ? e.getStudentPlan().getId() : null,
                        e.getStudentPlan() != null ? e.getStudentPlan().getPlan().getName() : null))
                .toList();

        return new ClassScheduleDetailResponse(
                s.getId(), s.getName(), s.getDayOfWeek(), s.getStartTime(),
                s.getDurationMinutes(), s.getTeacher().getId(), s.getTeacher().getName(),
                s.getType(), s.getMaxStudents(), s.getStatus(), students);
    }

    private ClassOccurrenceSummaryResponse toOccurrenceSummary(ClassOccurrence o) {
        return new ClassOccurrenceSummaryResponse(
                o.getId(), o.getClassSchedule().getId(), o.getClassSchedule().getName(),
                o.getOccurrenceDate(), o.getClassSchedule().getStartTime(),
                o.getClassSchedule().getDurationMinutes(),
                o.getTeacher().getId(), o.getTeacher().getName(),
                o.getStatus(), 0);
    }

    private ClassOccurrenceDetailResponse toOccurrenceDetail(ClassOccurrence o) {
        List<AttendanceRecordResponse> attendances = o.getAttendanceRecords().stream()
                .map(a -> new AttendanceRecordResponse(
                        a.getId(), a.getStudent().getId(), a.getStudent().getName(),
                        a.getStatus(), a.getStudentBilledValue(), a.getTeacherPayoutValue()))
                .toList();

        return new ClassOccurrenceDetailResponse(
                o.getId(), o.getClassSchedule().getId(), o.getClassSchedule().getName(),
                o.getOccurrenceDate(), o.getClassSchedule().getStartTime(),
                o.getClassSchedule().getDurationMinutes(),
                o.getTeacher().getId(), o.getTeacher().getName(),
                o.getStatus(), attendances);
    }
}
