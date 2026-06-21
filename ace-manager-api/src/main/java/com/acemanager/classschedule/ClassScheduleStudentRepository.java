package com.acemanager.classschedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassScheduleStudentRepository extends JpaRepository<ClassScheduleStudent, UUID> {

    @Query("""
        SELECT css FROM ClassScheduleStudent css
        JOIN FETCH css.student
        WHERE css.classSchedule.id = :scheduleId AND css.deletedAt IS NULL
        """)
    List<ClassScheduleStudent> findByScheduleId(@Param("scheduleId") UUID scheduleId);

    @Query("""
        SELECT css FROM ClassScheduleStudent css
        WHERE css.classSchedule.id = :scheduleId AND css.student.id = :studentId AND css.deletedAt IS NULL
        """)
    Optional<ClassScheduleStudent> findByScheduleAndStudent(
            @Param("scheduleId") UUID scheduleId,
            @Param("studentId") UUID studentId);

    @Query("SELECT COUNT(css) FROM ClassScheduleStudent css WHERE css.classSchedule.id = :scheduleId AND css.deletedAt IS NULL")
    long countEnrolled(@Param("scheduleId") UUID scheduleId);
}
