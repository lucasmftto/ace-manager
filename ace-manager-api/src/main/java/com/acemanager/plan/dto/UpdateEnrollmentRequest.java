package com.acemanager.plan.dto;

import com.acemanager.plan.StudentPlanStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateEnrollmentRequest(
        @DecimalMin("0") BigDecimal billedValue,
        StudentPlanStatus status
) {}
