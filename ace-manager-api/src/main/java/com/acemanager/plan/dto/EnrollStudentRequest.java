package com.acemanager.plan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EnrollStudentRequest(
        @NotNull UUID planId,
        UUID teacherId,
        @NotNull @DecimalMin("0") BigDecimal billedValue,
        @NotNull LocalDate startDate
) {}
