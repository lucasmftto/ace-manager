package com.acemanager.classschedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassOccurrenceRepository extends JpaRepository<ClassOccurrence, UUID> {

    boolean existsByClassScheduleIdAndOccurrenceDate(UUID classScheduleId, LocalDate occurrenceDate);

    @Query("""
        SELECT o FROM ClassOccurrence o
        JOIN FETCH o.classSchedule s
        JOIN FETCH o.teacher
        WHERE o.deletedAt IS NULL
          AND o.occurrenceDate >= :dateFrom
          AND o.occurrenceDate <= :dateTo
        ORDER BY o.occurrenceDate, s.startTime
        """)
    List<ClassOccurrence> findByDateRange(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo")   LocalDate dateTo);

    @Query("""
        SELECT o FROM ClassOccurrence o
        JOIN FETCH o.classSchedule s
        JOIN FETCH o.teacher
        WHERE o.deletedAt IS NULL
        ORDER BY o.occurrenceDate, s.startTime
        """)
    List<ClassOccurrence> findAllActive();

    @Query("""
        SELECT DISTINCT o.id FROM AttendanceRecord a
        JOIN a.classOccurrence o
        WHERE a.student.id = :studentId AND o.deletedAt IS NULL
        """)
    List<UUID> findOccurrenceIdsByStudent(@Param("studentId") UUID studentId);

    @Query("""
        SELECT o FROM ClassOccurrence o
        JOIN FETCH o.classSchedule s
        JOIN FETCH o.teacher
        LEFT JOIN FETCH o.attendanceRecords ar
        LEFT JOIN FETCH ar.student
        WHERE o.id = :id AND o.deletedAt IS NULL
        """)
    Optional<ClassOccurrence> findDetailById(@Param("id") UUID id);

    @Query("""
        SELECT o FROM ClassOccurrence o
        WHERE o.classSchedule.id = :scheduleId AND o.deletedAt IS NULL
        ORDER BY o.occurrenceDate
        """)
    List<ClassOccurrence> findByScheduleId(@Param("scheduleId") UUID scheduleId);
}
