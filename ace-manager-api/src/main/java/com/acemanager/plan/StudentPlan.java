package com.acemanager.plan;

import com.acemanager.shared.audit.Auditable;
import com.acemanager.student.Student;
import com.acemanager.teacher.Teacher;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPlan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal billedValue;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer remainingClasses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentPlanStatus status;
}
