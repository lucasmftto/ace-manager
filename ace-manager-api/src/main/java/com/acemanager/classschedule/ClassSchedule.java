package com.acemanager.classschedule;

import com.acemanager.shared.audit.Auditable;
import com.acemanager.teacher.Teacher;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "class_schedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSchedule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassType type;

    @Column(nullable = false)
    private Integer maxStudents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassScheduleStatus status;

    @OneToMany(mappedBy = "classSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClassScheduleStudent> enrolledStudents = new ArrayList<>();
}
