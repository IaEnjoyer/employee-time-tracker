package com.iaenjoyer.employeetimetracker.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Data
public class TimeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column
    private String rejectionReason;

    @Column(length = 1000)
    private String notes;

    @Column
    private double hours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column
    private LocalDateTime approvalDate;

    @PrePersist
    @PreUpdate
    private void calculateHours() {
        if (startTime != null && endTime != null) {
            Duration duration = Duration.between(startTime, endTime);
            hours = duration.toMinutes() / 60.0;
        }
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    public double getHours() {
        return hours;
    }

    public double getTotalHours() {
        if (endTime == null) {
            return 0;
        }
        return Duration.between(startTime, endTime).toHours();
    }

    public boolean isApproved() {
        return status.equals(Status.APPROVED);
    }
}
