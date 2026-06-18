package com.acemanager.student.dto;

import com.acemanager.student.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateStudentRequest(
        @NotBlank String name,
        String phone,
        @Email String email,
        LocalDate birthDate,
        String guardianName,
        String guardianPhone,
        @NotNull @DecimalMin("0.0") BigDecimal agreedMonthlyValue,
        @NotNull @DecimalMin("0.0") BigDecimal currentMonthlyValue,
        @NotNull PaymentMethod preferredPaymentMethod,
        String notes
) {}
