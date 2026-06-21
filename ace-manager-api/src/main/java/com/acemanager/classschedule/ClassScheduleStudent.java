package com.acemanager.classschedule;

import com.acemanager.plan.StudentPlan;
import com.acemanager.shared.audit.Auditable;
import com.acemanager.student.Student;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_schedule_students",
       uniqueConstraints = @UniqueConstraint(columnNames = {"class_schedule_id", "student_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassScheduleStudent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_plan_id")
    private StudentPlan studentPlan;
}
