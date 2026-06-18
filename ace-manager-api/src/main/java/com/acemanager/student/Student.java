package com.acemanager.student;

import com.acemanager.shared.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;
    private LocalDate birthDate;

    private String guardianName;
    private String guardianPhone;

    @Column(precision = 10, scale = 2)
    private BigDecimal agreedMonthlyValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal currentMonthlyValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod preferredPaymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
