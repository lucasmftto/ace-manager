package com.acemanager.teacher.dto;

import com.acemanager.teacher.PayoutModel;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateTeacherRequest(
        @NotBlank String name,
        String phone,
        @Email String email,
        @NotNull PayoutModel payoutModel,
        @DecimalMin("0") @DecimalMax("100") BigDecimal defaultPercentage,
        @DecimalMin("0") BigDecimal defaultHourlyRate
) {}
