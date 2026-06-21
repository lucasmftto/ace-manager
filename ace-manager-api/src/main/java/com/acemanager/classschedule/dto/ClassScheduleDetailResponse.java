package com.acemanager.classschedule.dto;

import com.acemanager.classschedule.ClassScheduleStatus;
import com.acemanager.classschedule.ClassType;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ClassScheduleDetailResponse(
        UUID id,
        String name,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        Integer durationMinutes,
        UUID teacherId,
        String teacherName,
        ClassType type,
        Integer maxStudents,
        ClassScheduleStatus status,
        List<ScheduleStudentResponse> enrolledStudents
) {}
