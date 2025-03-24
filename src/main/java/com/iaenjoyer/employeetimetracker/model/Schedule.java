package com.iaenjoyer.employeetimetracker.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "schedules")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String department;
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType type;

    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakDuration;

    private boolean monday;
    private boolean tuesday;
    private boolean wednesday;
    private boolean thursday;
    private boolean friday;
    private boolean saturday;
    private boolean sunday;

    public enum ScheduleType {
        FULL_TIME,
        PART_TIME,
        FLEXIBLE,
        CUSTOM
    }
}
