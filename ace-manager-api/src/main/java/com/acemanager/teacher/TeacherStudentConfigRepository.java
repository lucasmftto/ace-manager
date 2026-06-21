package com.acemanager.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherStudentConfigRepository extends JpaRepository<TeacherStudentConfig, UUID> {

    @Query("SELECT c FROM TeacherStudentConfig c JOIN FETCH c.student WHERE c.teacher.id = :teacherId")
    List<TeacherStudentConfig> findAllByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT c FROM TeacherStudentConfig c WHERE c.teacher.id = :teacherId AND c.student.id = :studentId")
    Optional<TeacherStudentConfig> findByTeacherIdAndStudentId(
            @Param("teacherId") UUID teacherId,
            @Param("studentId") UUID studentId
    );
}
