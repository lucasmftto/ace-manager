package com.acemanager.plan;

import com.acemanager.BaseIntegrationTest;
import com.acemanager.plan.dto.CreatePlanRequest;
import com.acemanager.plan.dto.EnrollStudentRequest;
import com.acemanager.student.PaymentMethod;
import com.acemanager.student.Student;
import com.acemanager.student.StudentRepository;
import com.acemanager.student.StudentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(roles = "OWNER")
class PlanControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PlanRepository planRepository;
    @Autowired private StudentPlanRepository studentPlanRepository;
    @Autowired private StudentRepository studentRepository;

    @BeforeEach
    void setUp() {
        studentPlanRepository.deleteAll();
        planRepository.deleteAll();
        studentRepository.deleteAll();
    }

    // --- CREATE PLAN ---

    @Test
    void shouldCreatePlan_withMonthlyType() throws Exception {
        var request = new CreatePlanRequest("Individual Mensal", "Aula individual por mês",
                PlanType.MONTHLY, new BigDecimal("500.00"), 3, 10, null, null);

        mockMvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Individual Mensal"))
                .andExpect(jsonPath("$.type").value("MONTHLY"))
                .andExpect(jsonPath("$.billingDayOfMonth").value(10))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(planRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldCreatePlan_withBundleType() throws Exception {
        var request = new CreatePlanRequest("Pacote 10 aulas", null,
                PlanType.BUNDLE, new BigDecimal("800.00"), null, null, 10, null);

        mockMvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("BUNDLE"))
                .andExpect(jsonPath("$.totalClasses").value(10));
    }

    @Test
    void shouldReturn400_whenBillingDayExceeds28() throws Exception {
        var request = new CreatePlanRequest("Mensal", null,
                PlanType.MONTHLY, new BigDecimal("500.00"), 3, 29, null, null);

        mockMvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_whenBundlePlanMissingTotalClasses() throws Exception {
        var request = new CreatePlanRequest("Pacote", null,
                PlanType.BUNDLE, new BigDecimal("500.00"), null, null, null, null);

        mockMvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    // --- ENROLLMENT ---

    @Test
    void shouldEnrollStudentInPlan_successfully() throws Exception {
        Plan plan = savePlan("Individual Mensal", PlanType.MONTHLY, new BigDecimal("500.00"), 5, 10, null, null);
        Student student = saveStudent("Marcos Lima");

        var request = new EnrollStudentRequest(plan.getId(), null,
                new BigDecimal("450.00"), LocalDate.now());

        mockMvc.perform(post("/api/v1/students/{id}/plans", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planName").value("Individual Mensal"))
                .andExpect(jsonPath("$.billedValue").value(450.00))
                .andExpect(jsonPath("$.discountAmount").value(50.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn400_whenBilledValueExceedsReferencePrice() throws Exception {
        Plan plan = savePlan("Individual Mensal", PlanType.MONTHLY, new BigDecimal("500.00"), 3, 10, null, null);
        Student student = saveStudent("Marcos Lima");

        var request = new EnrollStudentRequest(plan.getId(), null,
                new BigDecimal("600.00"), LocalDate.now());

        mockMvc.perform(post("/api/v1/students/{id}/plans", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    void shouldBlockEnrollment_whenGroupPlanAtCapacity() throws Exception {
        Plan plan = savePlan("Turma A", PlanType.GROUP, new BigDecimal("300.00"), null, null, null, 1);
        Student student1 = saveStudent("Aluno 1");
        Student student2 = saveStudent("Aluno 2");

        var req = new EnrollStudentRequest(plan.getId(), null, new BigDecimal("300.00"), LocalDate.now());

        mockMvc.perform(post("/api/v1/students/{id}/plans", student1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/students/{id}/plans", student2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    void shouldSetRemainingClasses_whenEnrollingInBundle() throws Exception {
        Plan plan = savePlan("Pacote 10", PlanType.BUNDLE, new BigDecimal("800.00"), null, null, 10, null);
        Student student = saveStudent("Marcos Lima");

        var request = new EnrollStudentRequest(plan.getId(), null,
                new BigDecimal("800.00"), LocalDate.now());

        mockMvc.perform(post("/api/v1/students/{id}/plans", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingClasses").value(10))
                .andExpect(jsonPath("$.lowClassesAlert").value(false));
    }

    @Test
    void shouldCancelEnrollment_setsStatusToCancelled() throws Exception {
        Plan plan = savePlan("Individual Mensal", PlanType.MONTHLY, new BigDecimal("500.00"), 3, 10, null, null);
        Student student = saveStudent("Marcos Lima");

        var enrollReq = new EnrollStudentRequest(plan.getId(), null,
                new BigDecimal("500.00"), LocalDate.now());

        String body = mockMvc.perform(post("/api/v1/students/{id}/plans", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String enrollmentId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(delete("/api/v1/students/{sid}/plans/{eid}",
                        student.getId(), enrollmentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/students/{id}/plans", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnActiveEnrollments_forStudent() throws Exception {
        Plan plan = savePlan("Individual Mensal", PlanType.MONTHLY, new BigDecimal("500.00"), 3, 10, null, null);
        Student student = saveStudent("Marcos Lima");

        var req = new EnrollStudentRequest(plan.getId(), null,
                new BigDecimal("500.00"), LocalDate.now());

        mockMvc.perform(post("/api/v1/students/{id}/plans", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/students/{id}/plans", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planName").value("Individual Mensal"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    // --- Helpers ---

    private Plan savePlan(String name, PlanType type, BigDecimal price,
                          Integer weekly, Integer billingDay, Integer totalClasses, Integer maxStudents) {
        return planRepository.save(Plan.builder()
                .name(name).type(type).referencePrice(price)
                .weeklyClassCount(weekly).billingDayOfMonth(billingDay)
                .totalClasses(totalClasses).maxStudents(maxStudents)
                .status(PlanStatus.ACTIVE).build());
    }

    private Student saveStudent(String name) {
        return studentRepository.save(Student.builder()
                .name(name)
                .preferredPaymentMethod(PaymentMethod.PIX)
                .agreedMonthlyValue(new BigDecimal("500.00"))
                .currentMonthlyValue(new BigDecimal("500.00"))
                .status(StudentStatus.ACTIVE).build());
    }
}
