package com.acemanager.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {

    @Query("SELECT s FROM Student s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Student> findActiveById(@Param("id") UUID id);

    @Query("SELECT s FROM Student s WHERE s.email = :email AND s.deletedAt IS NULL")
    Optional<Student> findActiveByEmail(@Param("email") String email);

    @Query("SELECT s FROM Student s WHERE s.deletedAt IS NULL")
    Page<Student> findAllActive(Pageable pageable);
}
