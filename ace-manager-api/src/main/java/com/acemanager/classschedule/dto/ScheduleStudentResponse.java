package com.acemanager.classschedule.dto;

import java.util.UUID;

public record ScheduleStudentResponse(
        UUID id,
        UUID studentId,
        String studentName,
        UUID studentPlanId,
        String studentPlanName
) {}
