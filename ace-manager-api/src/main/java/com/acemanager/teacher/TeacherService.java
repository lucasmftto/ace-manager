package com.acemanager.teacher;

import com.acemanager.teacher.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TeacherService {

    Page<TeacherSummaryResponse> findAll(Pageable pageable);

    TeacherDetailResponse findById(UUID id);

    TeacherDetailResponse create(CreateTeacherRequest request);

    TeacherDetailResponse update(UUID id, UpdateTeacherRequest request);

    void delete(UUID id);

    List<StudentPayoutConfigResponse> findStudentConfigs(UUID teacherId);

    StudentPayoutConfigResponse upsertStudentConfig(UUID teacherId, UUID studentId, UpsertStudentConfigRequest request);

    void removeStudentConfig(UUID teacherId, UUID studentId);

    BigDecimal resolveEffectivePayout(UUID teacherId, UUID studentId, BigDecimal studentValue, int durationMinutes);
}
