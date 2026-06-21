package com.acemanager.plan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    @Query("SELECT p FROM Plan p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Plan> findActiveById(@Param("id") UUID id);

    @Query("SELECT p FROM Plan p WHERE p.deletedAt IS NULL")
    Page<Plan> findAllActive(Pageable pageable);

    @Query("SELECT COUNT(sp) FROM StudentPlan sp WHERE sp.plan.id = :planId AND sp.status = 'ACTIVE'")
    long countActiveEnrollments(@Param("planId") UUID planId);
}
