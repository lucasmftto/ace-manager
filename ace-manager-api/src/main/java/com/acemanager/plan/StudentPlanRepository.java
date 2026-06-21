package com.acemanager.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentPlanRepository extends JpaRepository<StudentPlan, UUID> {

    @Query("SELECT sp FROM StudentPlan sp JOIN FETCH sp.plan WHERE sp.student.id = :studentId AND sp.status = 'ACTIVE'")
    List<StudentPlan> findActiveByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT sp FROM StudentPlan sp WHERE sp.id = :id AND sp.student.id = :studentId")
    Optional<StudentPlan> findByIdAndStudentId(@Param("id") UUID id, @Param("studentId") UUID studentId);

    @Query("SELECT COUNT(sp) FROM StudentPlan sp WHERE sp.plan.id = :planId AND sp.status = 'ACTIVE'")
    long countActiveByPlanId(@Param("planId") UUID planId);
}
