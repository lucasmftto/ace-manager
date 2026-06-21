package com.acemanager.classschedule.dto;

import com.acemanager.classschedule.ClassScheduleStatus;
import com.acemanager.classschedule.ClassType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record UpdateClassScheduleRequest(
        @NotBlank String name,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull @Min(30) Integer durationMinutes,
        @NotNull UUID teacherId,
        @NotNull ClassType type,
        Integer maxStudents,
        @NotNull ClassScheduleStatus status
) {}
