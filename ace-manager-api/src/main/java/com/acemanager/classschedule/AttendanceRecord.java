package com.acemanager.classschedule;

import com.acemanager.shared.audit.Auditable;
import com.acemanager.student.Student;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "attendance_records",
       uniqueConstraints = @UniqueConstraint(columnNames = {"class_occurrence_id", "student_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_occurrence_id", nullable = false)
    private ClassOccurrence classOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column(precision = 10, scale = 2)
    private BigDecimal studentBilledValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal teacherPayoutValue;
}
