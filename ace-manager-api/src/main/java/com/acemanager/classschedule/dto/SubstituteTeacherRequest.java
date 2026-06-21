package com.acemanager.classschedule.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubstituteTeacherRequest(@NotNull UUID teacherId) {}
