package com.acemanager.classschedule.dto;

import com.acemanager.classschedule.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpdateAttendanceRequest(
        @NotNull List<AttendanceEntry> attendances
) {
    public record AttendanceEntry(
            @NotNull UUID studentId,
            @NotNull AttendanceStatus status
    ) {}
}
