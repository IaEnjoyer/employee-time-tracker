package com.iaenjoyer.employeetimetracker.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Duration;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TimeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false,name="hora_entrada")
    private LocalDateTime startTime;

    @Column(name="hora_salida")
    private LocalDateTime endTime;

    @Column
    private String rejectionReason;

    @Column(name="horas_estipuladas")
    private Double hours;

    @Column(name="horas_ordinarias")
    private Double hoursOrdinary =8d;

    @Column(name="dispositivo")
    private String dispositivo;

    @Column(name="ip")
    private String ip;

    @PrePersist
    @PreUpdate
    private void calculateHours() {
        if (startTime != null && endTime != null) {
            Duration duration = Duration.between(startTime, endTime);
            hours = duration.toMinutes() / 60.0;
        }
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
}
