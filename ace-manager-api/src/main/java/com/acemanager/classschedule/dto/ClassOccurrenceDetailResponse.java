package com.acemanager.classschedule.dto;

import com.acemanager.classschedule.OccurrenceStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ClassOccurrenceDetailResponse(
        UUID id,
        UUID scheduleId,
        String scheduleName,
        LocalDate occurrenceDate,
        LocalTime startTime,
        Integer durationMinutes,
        UUID teacherId,
        String teacherName,
        OccurrenceStatus status,
        List<AttendanceRecordResponse> attendances
) {}
