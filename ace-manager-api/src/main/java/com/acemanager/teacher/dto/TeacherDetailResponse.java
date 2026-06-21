package com.acemanager.teacher.dto;

import com.acemanager.teacher.PayoutModel;
import com.acemanager.teacher.TeacherStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TeacherDetailResponse(
        UUID id,
        String name,
        String phone,
        String email,
        PayoutModel payoutModel,
        BigDecimal defaultPercentage,
        BigDecimal defaultHourlyRate,
        TeacherStatus status,
        List<StudentPayoutConfigResponse> studentConfigs
) {}
