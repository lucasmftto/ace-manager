package com.acemanager.teacher.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentPayoutConfigResponse(
        UUID studentId,
        String studentName,
        BigDecimal effectivePercentage,
        BigDecimal effectiveHourlyRate,
        boolean isOverride
) {}
