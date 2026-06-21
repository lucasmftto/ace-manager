package com.acemanager.classschedule;

import com.acemanager.BaseIntegrationTest;
import com.acemanager.classschedule.dto.*;
import com.acemanager.plan.*;
import com.acemanager.student.PaymentMethod;
import com.acemanager.student.Student;
import com.acemanager.student.StudentRepository;
import com.acemanager.student.StudentStatus;
import com.acemanager.teacher.PayoutModel;
import com.acemanager.teacher.Teacher;
import com.acemanager.teacher.TeacherRepository;
import com.acemanager.teacher.TeacherStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(roles = "OWNER")
class ClassManagementControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ClassScheduleRepository scheduleRepo;
    @Autowired private ClassOccurrenceRepository occurrenceRepo;
    @Autowired private AttendanceRecordRepository attendanceRepo;
    @Autowired private ClassScheduleStudentRepository scheduleStudentRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private PlanRepository planRepo;
    @Autowired private StudentPlanRepository studentPlanRepo;

    private Teacher teacher;
    private Student student;

    @BeforeEach
    void setUp() {
        attendanceRepo.deleteAll();
        occurrenceRepo.deleteAll();
        scheduleStudentRepo.deleteAll();
        scheduleRepo.deleteAll();
        studentPlanRepo.deleteAll();
        planRepo.deleteAll();
        studentRepo.deleteAll();
        teacherRepo.deleteAll();

        teacher = teacherRepo.save(Teacher.builder()
                .name("Prof. João")
                .payoutModel(PayoutModel.PERCENTAGE)
                .defaultPercentage(new BigDecimal("60.00"))
                .status(TeacherStatus.ACTIVE)
                .build());

        student = studentRepo.save(Student.builder()
                .name("Marcos Lima")
                .preferredPaymentMethod(PaymentMethod.PIX)
                .currentMonthlyValue(new BigDecimal("300.00"))
                .status(StudentStatus.ACTIVE)
                .build());
    }

    // ---- SCHEDULE ----

    @Test
    void shouldCreateClassSchedule_withValidData() throws Exception {
        var request = new CreateClassScheduleRequest(
                "Marcos - Individual - Terça 07h",
                DayOfWeek.TUESDAY,
                LocalTime.of(7, 0),
                60,
                teacher.getId(),
                ClassType.INDIVIDUAL,
                null
        );

        mockMvc.perform(post("/api/v1/class-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Marcos - Individual - Terça 07h"))
                .andExpect(jsonPath("$.type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.maxStudents").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(scheduleRepo.count()).isEqualTo(1);
    }

    @Test
    void shouldEnforceMaxStudents1_whenTypeIsIndividual() throws Exception {
        var request = new CreateClassScheduleRequest(
                "Aula Individual", DayOfWeek.MONDAY, LocalTime.of(8, 0),
                60, teacher.getId(), ClassType.INDIVIDUAL, 5
        );

        mockMvc.perform(post("/api/v1/class-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxStudents").value(1));
    }

    @Test
    void shouldAddStudentToSchedule_successfully() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);
        var request = new AddStudentToScheduleRequest(student.getId(), null);

        mockMvc.perform(post("/api/v1/class-schedules/{id}/students", schedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledStudents[0].studentName").value("Marcos Lima"));
    }

    @Test
    void shouldRejectStudent_whenGroupAtCapacity() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.GROUP, 1);

        Student student2 = studentRepo.save(Student.builder()
                .name("Outro Aluno").preferredPaymentMethod(PaymentMethod.PIX)
                .status(StudentStatus.ACTIVE).build());

        scheduleStudentRepo.save(ClassScheduleStudent.builder()
                .classSchedule(schedule).student(student).build());

        var request = new AddStudentToScheduleRequest(student2.getId(), null);

        mockMvc.perform(post("/api/v1/class-schedules/{id}/students", schedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- OCCURRENCES ----

    @Test
    void shouldGenerateOccurrences_forDateRange() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);

        // Monday 2024-01-01 → 2024-01-14: 2 Mondays
        var request = new GenerateOccurrencesRequest(
                schedule.getId(), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 14));

        mockMvc.perform(post("/api/v1/class-occurrences/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated").value(2))
                .andExpect(jsonPath("$.skipped").value(0));

        assertThat(occurrenceRepo.count()).isEqualTo(2);
    }

    @Test
    void shouldNotDuplicateOccurrence_forSameScheduleAndDate() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);
        var request = new GenerateOccurrencesRequest(
                schedule.getId(), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 7));

        mockMvc.perform(post("/api/v1/class-occurrences/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.generated").value(1));

        // Second call — same range
        mockMvc.perform(post("/api/v1/class-occurrences/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.generated").value(0))
                .andExpect(jsonPath("$.skipped").value(1));

        assertThat(occurrenceRepo.count()).isEqualTo(1);
    }

    @Test
    void shouldSubstituteTeacher_withoutChangingScheduleDefault() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);
        ClassOccurrence occurrence = createOccurrence(schedule, LocalDate.of(2024, 1, 1));

        Teacher substitute = teacherRepo.save(Teacher.builder()
                .name("Substituto").payoutModel(PayoutModel.HOURLY_RATE)
                .defaultHourlyRate(new BigDecimal("100.00")).status(TeacherStatus.ACTIVE).build());

        var request = new SubstituteTeacherRequest(substitute.getId());

        mockMvc.perform(patch("/api/v1/class-occurrences/{id}/teacher", occurrence.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherName").value("Substituto"));

        // Original schedule teacher unchanged — use query with JOIN FETCH to avoid LazyInit
        ClassSchedule reloaded = scheduleRepo.findActiveById(schedule.getId()).orElseThrow();
        assertThat(reloaded.getTeacher().getName()).isEqualTo("Prof. João");
    }

    // ---- ATTENDANCE ----

    @Test
    void shouldMarkAttendance_andComputePayoutValue() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);
        scheduleStudentRepo.save(ClassScheduleStudent.builder()
                .classSchedule(schedule).student(student).build());
        ClassOccurrence occurrence = createOccurrence(schedule, LocalDate.of(2024, 1, 1));

        var request = new UpdateAttendanceRequest(
                List.of(new UpdateAttendanceRequest.AttendanceEntry(student.getId(), AttendanceStatus.PRESENT)));

        mockMvc.perform(put("/api/v1/class-occurrences/{id}/attendance", occurrence.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.attendances[0].status").value("PRESENT"))
                .andExpect(jsonPath("$.attendances[0].studentBilledValue").value(300.00))
                .andExpect(jsonPath("$.attendances[0].teacherPayoutValue").value(180.00));
    }

    @Test
    void shouldDecrementRemainingClasses_whenBundlePlanStudentAttends() throws Exception {
        Plan plan = planRepo.save(Plan.builder()
                .name("Pacote 10").type(PlanType.BUNDLE).referencePrice(new BigDecimal("500.00"))
                .totalClasses(10).status(PlanStatus.ACTIVE).build());

        StudentPlan studentPlan = studentPlanRepo.save(StudentPlan.builder()
                .student(student).plan(plan)
                .billedValue(new BigDecimal("450.00")).startDate(LocalDate.of(2024, 1, 1))
                .remainingClasses(10).status(StudentPlanStatus.ACTIVE).build());

        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);
        scheduleStudentRepo.save(ClassScheduleStudent.builder()
                .classSchedule(schedule).student(student).studentPlan(studentPlan).build());
        ClassOccurrence occurrence = createOccurrence(schedule, LocalDate.of(2024, 1, 1));

        var request = new UpdateAttendanceRequest(
                List.of(new UpdateAttendanceRequest.AttendanceEntry(student.getId(), AttendanceStatus.PRESENT)));

        mockMvc.perform(put("/api/v1/class-occurrences/{id}/attendance", occurrence.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        StudentPlan updated = studentPlanRepo.findById(studentPlan.getId()).orElseThrow();
        assertThat(updated.getRemainingClasses()).isEqualTo(9);
    }

    @Test
    void shouldNotGeneratePayout_whenStudentIsAbsent() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);
        scheduleStudentRepo.save(ClassScheduleStudent.builder()
                .classSchedule(schedule).student(student).build());
        ClassOccurrence occurrence = createOccurrence(schedule, LocalDate.of(2024, 1, 1));

        var request = new UpdateAttendanceRequest(
                List.of(new UpdateAttendanceRequest.AttendanceEntry(student.getId(), AttendanceStatus.ABSENT)));

        mockMvc.perform(put("/api/v1/class-occurrences/{id}/attendance", occurrence.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendances[0].status").value("ABSENT"))
                .andExpect(jsonPath("$.attendances[0].teacherPayoutValue").doesNotExist());
    }

    @Test
    void shouldCancelOccurrence_andMarkAllAttendancesAsCancelled() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);
        ClassOccurrence occurrence = createOccurrence(schedule, LocalDate.of(2024, 1, 1));

        attendanceRepo.save(AttendanceRecord.builder()
                .classOccurrence(occurrence).student(student)
                .status(AttendanceStatus.PRESENT)
                .studentBilledValue(new BigDecimal("300.00"))
                .teacherPayoutValue(new BigDecimal("180.00"))
                .build());

        mockMvc.perform(patch("/api/v1/class-occurrences/{id}/cancel", occurrence.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.attendances[0].status").value("ABSENT"))
                .andExpect(jsonPath("$.attendances[0].teacherPayoutValue").doesNotExist());
    }

    // ---- FILTERS ----

    @Test
    void shouldReturnOccurrences_filteredByTeacherAndDateRange() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);
        createOccurrence(schedule, LocalDate.of(2024, 1, 1));
        createOccurrence(schedule, LocalDate.of(2024, 1, 8));
        createOccurrence(schedule, LocalDate.of(2024, 2, 5)); // outside range

        mockMvc.perform(get("/api/v1/class-occurrences")
                        .param("teacherId", teacher.getId().toString())
                        .param("dateFrom", "2024-01-01")
                        .param("dateTo", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnOccurrences_filteredByStudent() throws Exception {
        ClassSchedule schedule = createSchedule(ClassType.INDIVIDUAL, 1);
        ClassOccurrence o1 = createOccurrence(schedule, LocalDate.of(2024, 1, 1));
        ClassOccurrence o2 = createOccurrence(schedule, LocalDate.of(2024, 1, 8));
        createOccurrence(schedule, LocalDate.of(2024, 1, 15)); // no attendance for this student

        attendanceRepo.save(AttendanceRecord.builder().classOccurrence(o1).student(student).status(AttendanceStatus.PRESENT).build());
        attendanceRepo.save(AttendanceRecord.builder().classOccurrence(o2).student(student).status(AttendanceStatus.ABSENT).build());

        mockMvc.perform(get("/api/v1/class-occurrences")
                        .param("studentId", student.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- helpers ----

    private ClassSchedule createSchedule(ClassType type, int maxStudents) {
        return scheduleRepo.save(ClassSchedule.builder()
                .name("Aula Teste").dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(7, 0)).durationMinutes(60)
                .teacher(teacher).type(type).maxStudents(maxStudents)
                .status(ClassScheduleStatus.ACTIVE).build());
    }

    private ClassOccurrence createOccurrence(ClassSchedule schedule, LocalDate date) {
        return occurrenceRepo.save(ClassOccurrence.builder()
                .classSchedule(schedule).occurrenceDate(date)
                .teacher(schedule.getTeacher()).status(OccurrenceStatus.SCHEDULED).build());
    }
}
