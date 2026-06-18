package com.acemanager.student;

import com.acemanager.student.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StudentService {

    Page<StudentSummaryResponse> findAll(StudentFilterParams filters, Pageable pageable);

    StudentDetailResponse findById(UUID id);

    StudentDetailResponse create(CreateStudentRequest request);

    StudentDetailResponse update(UUID id, UpdateStudentRequest request);

    void delete(UUID id);

    StudentDetailResponse findCurrentStudentProfile();
}
