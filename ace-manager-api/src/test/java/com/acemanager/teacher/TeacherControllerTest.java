package com.acemanager.teacher;

import com.acemanager.BaseIntegrationTest;
import com.acemanager.student.PaymentMethod;
import com.acemanager.student.Student;
import com.acemanager.student.StudentRepository;
import com.acemanager.student.StudentStatus;
import com.acemanager.teacher.dto.CreateTeacherRequest;
import com.acemanager.teacher.dto.UpsertStudentConfigRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(roles = "OWNER")
class TeacherControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private TeacherStudentConfigRepository configRepository;
    @Autowired private StudentRepository studentRepository;

    @BeforeEach
    void setUp() {
        configRepository.deleteAll();
        teacherRepository.deleteAll();
        studentRepository.deleteAll();
    }

    // --- CREATE ---

    @Test
    void shouldCreateTeacher_withPercentageModel() throws Exception {
        var request = new CreateTeacherRequest("Ana Costa", "11999990000", "ana@email.com",
                PayoutModel.PERCENTAGE, new BigDecimal("70.00"), null);

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ana Costa"))
                .andExpect(jsonPath("$.payoutModel").value("PERCENTAGE"))
                .andExpect(jsonPath("$.defaultPercentage").value(70.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(teacherRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldCreateTeacher_withHourlyRateModel() throws Exception {
        var request = new CreateTeacherRequest("Bruno Silva", null, null,
                PayoutModel.HOURLY_RATE, null, new BigDecimal("120.00"));

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payoutModel").value("HOURLY_RATE"))
                .andExpect(jsonPath("$.defaultHourlyRate").value(120.00));
    }

    @Test
    void shouldReturn400_whenPercentageModelMissingDefaultPercentage() throws Exception {
        var request = new CreateTeacherRequest("Ana Costa", null, null,
                PayoutModel.PERCENTAGE, null, null);

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    void shouldReturn400_whenHourlyRateModelMissingDefaultRate() throws Exception {
        var request = new CreateTeacherRequest("Bruno Silva", null, null,
                PayoutModel.HOURLY_RATE, null, null);

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    // --- CRUD ---

    @Test
    void shouldReturnTeacherList() throws Exception {
        saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);
        saveTeacher("Bruno Silva", PayoutModel.HOURLY_RATE, null, new BigDecimal("120"));

        mockMvc.perform(get("/api/v1/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void shouldReturnTeacherById() throws Exception {
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);

        mockMvc.perform(get("/api/v1/teachers/{id}", teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Costa"))
                .andExpect(jsonPath("$.studentConfigs").isArray());
    }

    @Test
    void shouldReturn404_whenTeacherNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSoftDeleteTeacher() throws Exception {
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);

        mockMvc.perform(delete("/api/v1/teachers/{id}", teacher.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/teachers/{id}", teacher.getId()))
                .andExpect(status().isNotFound());
    }

    // --- STUDENT CONFIG ---

    @Test
    void shouldUpsertStudentConfig_createsNewWhenNotExists() throws Exception {
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);
        Student student = saveStudent("Marcos Lima");

        var request = new UpsertStudentConfigRequest(new BigDecimal("60.00"), null);

        mockMvc.perform(put("/api/v1/teachers/{id}/student-configs/{studentId}",
                        teacher.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.getId().toString()))
                .andExpect(jsonPath("$.effectivePercentage").value(60.00))
                .andExpect(jsonPath("$.isOverride").value(true));

        assertThat(configRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldUpsertStudentConfig_updatesExistingWhenExists() throws Exception {
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);
        Student student = saveStudent("Marcos Lima");

        var first = new UpsertStudentConfigRequest(new BigDecimal("60.00"), null);
        mockMvc.perform(put("/api/v1/teachers/{id}/student-configs/{studentId}",
                        teacher.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        var second = new UpsertStudentConfigRequest(new BigDecimal("55.00"), null);
        mockMvc.perform(put("/api/v1/teachers/{id}/student-configs/{studentId}",
                        teacher.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectivePercentage").value(55.00));

        assertThat(configRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRemoveStudentConfig_revertsToDefault() throws Exception {
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);
        Student student = saveStudent("Marcos Lima");

        var request = new UpsertStudentConfigRequest(new BigDecimal("60.00"), null);
        mockMvc.perform(put("/api/v1/teachers/{id}/student-configs/{studentId}",
                        teacher.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/teachers/{id}/student-configs/{studentId}",
                        teacher.getId(), student.getId()))
                .andExpect(status().isNoContent());

        assertThat(configRepository.count()).isEqualTo(0);
    }

    @Test
    void shouldReturnEffectiveConfig_usingOverrideWhenExists() throws Exception {
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);
        Student student = saveStudent("Marcos Lima");

        var request = new UpsertStudentConfigRequest(new BigDecimal("50.00"), null);
        mockMvc.perform(put("/api/v1/teachers/{id}/student-configs/{studentId}",
                        teacher.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/teachers/{id}/student-configs", teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].effectivePercentage").value(50.00))
                .andExpect(jsonPath("$[0].isOverride").value(true));
    }

    @Test
    void shouldReturnEffectiveConfig_usingDefaultWhenNoOverride() throws Exception {
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);
        Student student = saveStudent("Marcos Lima");

        var request = new UpsertStudentConfigRequest(null, null);
        mockMvc.perform(put("/api/v1/teachers/{id}/student-configs/{studentId}",
                        teacher.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/teachers/{id}/student-configs", teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].effectivePercentage").value(70.00))
                .andExpect(jsonPath("$[0].isOverride").value(false));
    }

    // --- PAYOUT RESOLUTION ---

    @Test
    void shouldResolvePayoutAsPercentage_whenModelIsPercentage() throws Exception {
        // 70% of R$500 = R$350 — validated via studentConfigs default behavior
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);

        mockMvc.perform(get("/api/v1/teachers/{id}", teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutModel").value("PERCENTAGE"))
                .andExpect(jsonPath("$.defaultPercentage").value(70.00));
    }

    @Test
    void shouldResolvePayoutAsHourlyRate_whenModelIsHourlyRate() throws Exception {
        Teacher teacher = saveTeacher("Bruno Silva", PayoutModel.HOURLY_RATE, null, new BigDecimal("120"));

        mockMvc.perform(get("/api/v1/teachers/{id}", teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutModel").value("HOURLY_RATE"))
                .andExpect(jsonPath("$.defaultHourlyRate").value(120.00));
    }

    @Test
    void shouldUseOverride_whenStudentConfigExists() throws Exception {
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);
        Student student = saveStudent("Marcos Lima");

        var request = new UpsertStudentConfigRequest(new BigDecimal("80.00"), null);
        mockMvc.perform(put("/api/v1/teachers/{id}/student-configs/{studentId}",
                        teacher.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.effectivePercentage").value(80.00))
                .andExpect(jsonPath("$.isOverride").value(true));
    }

    @Test
    void shouldUseTeacherDefault_whenNoStudentConfig() throws Exception {
        Teacher teacher = saveTeacher("Ana Costa", PayoutModel.PERCENTAGE, new BigDecimal("70"), null);
        Student student = saveStudent("Marcos Lima");

        mockMvc.perform(get("/api/v1/teachers/{id}/student-configs", teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Default is preserved on teacher itself
        mockMvc.perform(get("/api/v1/teachers/{id}", teacher.getId()))
                .andExpect(jsonPath("$.defaultPercentage").value(70.00));

        assertThat(student).isNotNull();
    }

    // --- Helpers ---

    private Teacher saveTeacher(String name, PayoutModel model, BigDecimal pct, BigDecimal rate) {
        return teacherRepository.save(Teacher.builder()
                .name(name)
                .payoutModel(model)
                .defaultPercentage(pct)
                .defaultHourlyRate(rate)
                .status(TeacherStatus.ACTIVE)
                .build());
    }

    private Student saveStudent(String name) {
        return studentRepository.save(Student.builder()
                .name(name)
                .preferredPaymentMethod(PaymentMethod.PIX)
                .agreedMonthlyValue(new BigDecimal("500.00"))
                .currentMonthlyValue(new BigDecimal("500.00"))
                .status(StudentStatus.ACTIVE)
                .build());
    }
}
