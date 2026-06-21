package com.acemanager.teacher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    @Query("SELECT t FROM Teacher t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<Teacher> findActiveById(@Param("id") UUID id);

    @Query("SELECT t FROM Teacher t WHERE t.deletedAt IS NULL")
    Page<Teacher> findAllActive(Pageable pageable);

    @Query("SELECT COUNT(c) FROM TeacherStudentConfig c WHERE c.teacher.id = :teacherId")
    long countStudentConfigs(@Param("teacherId") UUID teacherId);
}
