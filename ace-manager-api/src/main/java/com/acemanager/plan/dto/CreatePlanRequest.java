package com.acemanager.plan.dto;

import com.acemanager.plan.PlanType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePlanRequest(
        @NotBlank String name,
        String description,
        @NotNull PlanType type,
        @NotNull @DecimalMin("0") BigDecimal referencePrice,
        Integer weeklyClassCount,
        @Min(1) @Max(28) Integer billingDayOfMonth,
        @Min(1) Integer totalClasses,
        @Min(1) Integer maxStudents
) {}
