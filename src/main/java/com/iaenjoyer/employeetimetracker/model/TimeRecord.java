package com.iaenjoyer.employeetimetracker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "time_records")
public class TimeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(nullable = false)
    private double hours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeRecordStatus status = TimeRecordStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column
    private LocalDateTime approvalDate;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 1000)
    private String notes;

    @PrePersist
    @PreUpdate
    private void calculateHours() {
        if (startTime != null && endTime != null) {
            Duration duration = Duration.between(startTime, endTime);
            hours = duration.toMinutes() / 60.0;
        }
    }

    public enum TimeRecordStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public boolean isApproved() {
        return status.equals(TimeRecordStatus.APPROVED);
    }
}
