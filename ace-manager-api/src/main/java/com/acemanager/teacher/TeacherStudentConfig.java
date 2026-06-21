package com.acemanager.teacher;

import com.acemanager.shared.audit.Auditable;
import com.acemanager.student.Student;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "teacher_student_configs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "student_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherStudentConfig extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(precision = 5, scale = 2)
    private BigDecimal overridePercentage;

    @Column(precision = 10, scale = 2)
    private BigDecimal overrideHourlyRate;
}
