package com.acemanager.student;

import com.acemanager.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(roles = "OWNER")
class StudentControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        studentRepository.deleteAll();
    }

    // --- CREATE ---

    @Test
    void shouldCreateStudent_whenValidRequest() throws Exception {
        var request = buildCreateRequest("Marcos Lima", "marcos@email.com",
                new BigDecimal("500.00"), new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Marcos Lima"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.discountAmount").value(0))
                .andExpect(jsonPath("$.id").isNotEmpty());

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturn400_whenNameIsBlank() throws Exception {
        var request = buildCreateRequest("", "marcos@email.com",
                new BigDecimal("500.00"), new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn400_whenCurrentValueExceedsAgreedValue() throws Exception {
        var request = buildCreateRequest("Marcos Lima", "marcos@email.com",
                new BigDecimal("400.00"), new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    void shouldRequireGuardian_whenStudentIsMinor() throws Exception {
        var request = new com.acemanager.student.dto.CreateStudentRequest(
                "João Menor", "(11) 99999-0000", "joao@email.com",
                LocalDate.now().minusYears(10),
                null, null,
                new BigDecimal("300.00"), new BigDecimal("300.00"),
                PaymentMethod.PIX, null
        );

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    // --- READ ---

    @Test
    void shouldReturnStudentDetail_whenFound() throws Exception {
        Student student = persistStudent("Ana Silva", "ana@email.com");

        mockMvc.perform(get("/api/v1/students/{id}", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.email").value("ana@email.com"));
    }

    @Test
    void shouldReturn404_whenStudentNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldListStudents_withPagination() throws Exception {
        persistStudent("Abel Costa", "abel@email.com");
        persistStudent("Bia Rocha", "bia@email.com");
        persistStudent("Carlos Melo", "carlos@email.com");

        mockMvc.perform(get("/api/v1/students?page=0&size=2&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Abel Costa"));
    }

    @Test
    void shouldFilterStudents_byStatus() throws Exception {
        persistStudent("Ativo Um", "ativo1@email.com");
        persistStudentWithStatus("Inativo Um", "inativo1@email.com", StudentStatus.INACTIVE);

        mockMvc.perform(get("/api/v1/students?status=ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Ativo Um"));
    }

    @Test
    void shouldFilterStudents_byNameSearch() throws Exception {
        persistStudent("Marcos Lima", "marcos@email.com");
        persistStudent("Ana Paula", "ana@email.com");

        mockMvc.perform(get("/api/v1/students?search=marcos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Marcos Lima"));
    }

    // --- UPDATE ---

    @Test
    void shouldUpdateStudent_whenValidRequest() throws Exception {
        Student student = persistStudent("Nome Antigo", "antigo@email.com");

        var request = new com.acemanager.student.dto.UpdateStudentRequest(
                "Nome Novo", "(11) 99999-1111", "novo@email.com",
                null, null, null,
                new BigDecimal("600.00"), new BigDecimal("500.00"),
                PaymentMethod.PIX, StudentStatus.ACTIVE, null
        );

        mockMvc.perform(put("/api/v1/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nome Novo"))
                .andExpect(jsonPath("$.discountAmount").value(100.00));
    }

    @Test
    void shouldReturn404_whenUpdatingNonExistentStudent() throws Exception {
        var request = new com.acemanager.student.dto.UpdateStudentRequest(
                "Nome", null, null, null, null, null,
                new BigDecimal("500.00"), new BigDecimal("500.00"),
                PaymentMethod.PIX, StudentStatus.ACTIVE, null
        );

        mockMvc.perform(put("/api/v1/students/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE ---

    @Test
    void shouldSoftDeleteStudent_whenExists() throws Exception {
        Student student = persistStudent("Para Deletar", "delete@email.com");

        mockMvc.perform(delete("/api/v1/students/{id}", student.getId()))
                .andExpect(status().isNoContent());

        Student deleted = studentRepository.findById(student.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldNotReturnDeletedStudents_inListing() throws Exception {
        Student student = persistStudent("Para Deletar", "delete@email.com");

        mockMvc.perform(delete("/api/v1/students/{id}", student.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    // --- ROLE ---

    @Test
    @WithMockUser(roles = "TEACHER")
    void shouldReturn403_whenTeacherTriesToListAllStudents() throws Exception {
        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "aluno@email.com", roles = "STUDENT")
    void shouldReturnOwnProfile_whenStudentCallsMeEndpoint() throws Exception {
        persistStudentWithEmail("aluno@email.com");

        mockMvc.perform(get("/api/v1/students/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("aluno@email.com"));
    }

    // --- Helpers ---

    private Student persistStudent(String name, String email) {
        return studentRepository.save(Student.builder()
                .name(name)
                .email(email)
                .agreedMonthlyValue(new BigDecimal("500.00"))
                .currentMonthlyValue(new BigDecimal("500.00"))
                .preferredPaymentMethod(PaymentMethod.PIX)
                .status(StudentStatus.ACTIVE)
                .build());
    }

    private Student persistStudentWithStatus(String name, String email, StudentStatus status) {
        return studentRepository.save(Student.builder()
                .name(name)
                .email(email)
                .agreedMonthlyValue(new BigDecimal("500.00"))
                .currentMonthlyValue(new BigDecimal("500.00"))
                .preferredPaymentMethod(PaymentMethod.PIX)
                .status(status)
                .build());
    }

    private Student persistStudentWithEmail(String email) {
        return studentRepository.save(Student.builder()
                .name("Aluno Teste")
                .email(email)
                .agreedMonthlyValue(new BigDecimal("500.00"))
                .currentMonthlyValue(new BigDecimal("500.00"))
                .preferredPaymentMethod(PaymentMethod.PIX)
                .status(StudentStatus.ACTIVE)
                .build());
    }

    private com.acemanager.student.dto.CreateStudentRequest buildCreateRequest(
            String name, String email, BigDecimal agreed, BigDecimal current) {
        return new com.acemanager.student.dto.CreateStudentRequest(
                name, "(11) 99876-5432", email,
                LocalDate.of(1990, 1, 1),
                null, null,
                agreed, current,
                PaymentMethod.PIX, null
        );
    }
}
