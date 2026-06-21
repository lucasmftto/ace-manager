package com.acemanager.plan;

import com.acemanager.shared.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType type;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal referencePrice;

    private Integer weeklyClassCount;
    private Integer billingDayOfMonth;
    private Integer totalClasses;
    private Integer maxStudents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status;
}
