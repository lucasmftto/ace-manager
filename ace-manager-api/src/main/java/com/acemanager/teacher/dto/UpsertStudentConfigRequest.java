package com.acemanager.teacher.dto;

import java.math.BigDecimal;

public record UpsertStudentConfigRequest(
        BigDecimal overridePercentage,
        BigDecimal overrideHourlyRate
) {}
