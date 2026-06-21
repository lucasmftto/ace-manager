package com.acemanager.classschedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    @Query("SELECT a FROM AttendanceRecord a WHERE a.classOccurrence.id = :occurrenceId")
    List<AttendanceRecord> findByOccurrenceId(@Param("occurrenceId") UUID occurrenceId);

    @Query("SELECT a FROM AttendanceRecord a WHERE a.classOccurrence.id = :occurrenceId AND a.student.id = :studentId")
    Optional<AttendanceRecord> findByOccurrenceAndStudent(
            @Param("occurrenceId") UUID occurrenceId,
            @Param("studentId") UUID studentId);
}
