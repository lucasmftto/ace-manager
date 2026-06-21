package com.acemanager.classschedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, UUID> {

    @Query("""
        SELECT s FROM ClassSchedule s
        LEFT JOIN FETCH s.teacher
        WHERE s.deletedAt IS NULL
        """)
    Page<ClassSchedule> findAllActive(Pageable pageable);

    @Query("""
        SELECT s FROM ClassSchedule s
        LEFT JOIN FETCH s.teacher
        LEFT JOIN FETCH s.enrolledStudents es
        LEFT JOIN FETCH es.student
        LEFT JOIN FETCH es.studentPlan
        WHERE s.id = :id AND s.deletedAt IS NULL
        """)
    Optional<ClassSchedule> findActiveById(@Param("id") UUID id);

    @Query("""
        SELECT s FROM ClassSchedule s
        LEFT JOIN FETCH s.teacher
        WHERE s.teacher.id = :teacherId AND s.deletedAt IS NULL
        """)
    Page<ClassSchedule> findAllActiveByTeacherId(@Param("teacherId") UUID teacherId, Pageable pageable);
}
