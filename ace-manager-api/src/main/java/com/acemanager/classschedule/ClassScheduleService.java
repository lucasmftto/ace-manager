package com.acemanager.classschedule;

import com.acemanager.classschedule.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ClassScheduleService {

    Page<ClassScheduleSummaryResponse> findAll(Pageable pageable);
    ClassScheduleDetailResponse findById(UUID id);
    ClassScheduleSummaryResponse create(CreateClassScheduleRequest request);
    ClassScheduleDetailResponse update(UUID id, UpdateClassScheduleRequest request);
    void deactivate(UUID id);

    ClassScheduleDetailResponse addStudent(UUID scheduleId, AddStudentToScheduleRequest request);
    ClassScheduleDetailResponse removeStudent(UUID scheduleId, UUID studentId);

    GenerateOccurrencesResponse generateOccurrences(GenerateOccurrencesRequest request);
    List<ClassOccurrenceSummaryResponse> findOccurrences(LocalDate dateFrom, LocalDate dateTo,
                                                          UUID teacherId, UUID studentId,
                                                          OccurrenceStatus status);
    ClassOccurrenceDetailResponse findOccurrenceById(UUID id);
    ClassOccurrenceDetailResponse substituteTeacher(UUID occurrenceId, SubstituteTeacherRequest request);
    ClassOccurrenceDetailResponse cancelOccurrence(UUID occurrenceId);
    ClassOccurrenceDetailResponse updateAttendance(UUID occurrenceId, UpdateAttendanceRequest request);
}
