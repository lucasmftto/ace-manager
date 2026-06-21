package com.acemanager.plan.dto;

import com.acemanager.plan.PlanType;
import com.acemanager.plan.StudentPlanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StudentPlanResponse(
        UUID id,
        UUID planId,
        String planName,
        PlanType planType,
        UUID teacherId,
        String teacherName,
        BigDecimal referencePrice,
        BigDecimal billedValue,
        BigDecimal discountAmount,
        LocalDate startDate,
        LocalDate endDate,
        Integer remainingClasses,
        boolean lowClassesAlert,
        StudentPlanStatus status
) {}
