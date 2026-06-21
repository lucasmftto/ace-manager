package com.acemanager.teacher.dto;

import com.acemanager.teacher.PayoutModel;
import com.acemanager.teacher.TeacherStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record TeacherSummaryResponse(
        UUID id,
        String name,
        String phone,
        PayoutModel payoutModel,
        BigDecimal defaultPercentage,
        BigDecimal defaultHourlyRate,
        long studentConfigCount,
        TeacherStatus status
) {}
