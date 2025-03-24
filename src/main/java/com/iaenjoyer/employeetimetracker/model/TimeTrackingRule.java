package com.iaenjoyer.employeetimetracker.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Set;

@Data
@Entity
@Table(name = "time_tracking_rules")
public class TimeTrackingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean requireLocation = true;

    @ElementCollection
    @CollectionTable(name = "allowed_ips")
    private Set<String> allowedIps;

    @ElementCollection
    @CollectionTable(name = "allowed_devices")
    private Set<String> allowedDevices;

    @Column(nullable = false)
    private boolean allowManualEntry = false;

    @Column(nullable = false)
    private Integer maxLateMinutes = 15; // Minutos de tolerancia para retrasos

    @Column(nullable = false)
    private Integer earlyCheckoutMinutes = 5; // Minutos permitidos para salida anticipada

    @Column(nullable = false)
    private boolean requirePhoto = false;

    @Column(nullable = false)
    private boolean requireComment = false;

    @Column(nullable = false)
    private boolean notifyAdmin = true;

    @Column(nullable = false)
    private boolean notifySupervisor = true;
}
