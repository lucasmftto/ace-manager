package com.acemanager.plan.dto;

import com.acemanager.plan.PlanStatus;
import com.acemanager.plan.PlanType;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String name,
        String description,
        PlanType type,
        BigDecimal referencePrice,
        Integer weeklyClassCount,
        Integer billingDayOfMonth,
        Integer totalClasses,
        Integer maxStudents,
        long activeEnrollments,
        PlanStatus status
) {}
