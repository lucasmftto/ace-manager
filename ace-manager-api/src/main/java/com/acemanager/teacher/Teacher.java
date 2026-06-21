package com.acemanager.teacher;

import com.acemanager.shared.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Teacher extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutModel payoutModel;

    @Column(precision = 5, scale = 2)
    private BigDecimal defaultPercentage;

    @Column(precision = 10, scale = 2)
    private BigDecimal defaultHourlyRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherStatus status;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeacherStudentConfig> studentConfigs = new ArrayList<>();
}
