package com.acemanager.classschedule.dto;

import com.acemanager.classschedule.ClassScheduleStatus;
import com.acemanager.classschedule.ClassType;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record ClassScheduleSummaryResponse(
        UUID id,
        String name,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        Integer durationMinutes,
        UUID teacherId,
        String teacherName,
        ClassType type,
        Integer maxStudents,
        int enrolledCount,
        ClassScheduleStatus status
) {}
