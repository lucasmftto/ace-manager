package com.acemanager.student.dto;

import com.acemanager.student.PaymentMethod;
import com.acemanager.student.StudentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StudentDetailResponse(
        UUID id,
        String name,
        String phone,
        String email,
        LocalDate birthDate,
        String guardianName,
        String guardianPhone,
        BigDecimal agreedMonthlyValue,
        BigDecimal currentMonthlyValue,
        BigDecimal discountAmount,
        BigDecimal discountPercentage,
        PaymentMethod preferredPaymentMethod,
        StudentStatus status,
        String notes,
        LocalDateTime createdAt
) {}
