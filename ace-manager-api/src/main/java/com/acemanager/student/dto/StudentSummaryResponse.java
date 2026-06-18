package com.acemanager.student.dto;

import com.acemanager.student.PaymentMethod;
import com.acemanager.student.StudentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentSummaryResponse(
        UUID id,
        String name,
        String phone,
        BigDecimal currentMonthlyValue,
        PaymentMethod preferredPaymentMethod,
        StudentStatus status,
        String financialStatus
) {}
