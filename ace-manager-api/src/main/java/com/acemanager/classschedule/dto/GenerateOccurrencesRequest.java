package com.acemanager.classschedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateOccurrencesRequest(
        UUID scheduleId,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate
) {}
