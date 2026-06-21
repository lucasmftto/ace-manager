package com.acemanager.classschedule.dto;

import com.acemanager.classschedule.AttendanceStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record AttendanceRecordResponse(
        UUID id,
        UUID studentId,
        String studentName,
        AttendanceStatus status,
        BigDecimal studentBilledValue,
        BigDecimal teacherPayoutValue
) {}
