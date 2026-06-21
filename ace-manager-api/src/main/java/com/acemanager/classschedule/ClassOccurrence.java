package com.acemanager.classschedule;

import com.acemanager.shared.audit.Auditable;
import com.acemanager.teacher.Teacher;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "class_occurrences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassOccurrence extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @Column(nullable = false)
    private LocalDate occurrenceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccurrenceStatus status;

    @OneToMany(mappedBy = "classOccurrence", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AttendanceRecord> attendanceRecords = new ArrayList<>();
}
