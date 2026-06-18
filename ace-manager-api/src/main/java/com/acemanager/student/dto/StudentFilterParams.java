package com.acemanager.student.dto;

import com.acemanager.student.StudentStatus;

public record StudentFilterParams(
        StudentStatus status,
        String search
) {}
