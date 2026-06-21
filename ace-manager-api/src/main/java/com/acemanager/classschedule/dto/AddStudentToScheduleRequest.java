package com.acemanager.classschedule.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddStudentToScheduleRequest(
        @NotNull UUID studentId,
        UUID studentPlanId
) {}
