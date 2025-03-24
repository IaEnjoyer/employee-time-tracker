package com.iaenjoyer.employeetimetracker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.EMPLOYEE;

    @Column(nullable = false)
    private boolean dataConsent = false;

    @Column(nullable = false)
    private LocalDateTime consentDate;

    @Column(nullable = false)
    private int dataRetentionDays = 1460; // 4 años según la ley española

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    @ToString.Exclude
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    @ToString.Exclude
    private User supervisor;

    @OneToMany(mappedBy = "supervisor")
    @ToString.Exclude
    private Set<User> supervisedEmployees;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_tracking_rule_id")
    @ToString.Exclude
    private TimeTrackingRule timeTrackingRule;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<TimeRecord> timeRecords;

    @OneToMany(mappedBy = "reporter", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<Incident> reportedIncidents;

    @OneToMany(mappedBy = "assignee", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<Incident> assignedIncidents;

    @Column(unique = true, nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private LocalDateTime hireDate;

    @Column(length = 1000)
    private String notes;

    private String photoUrl;

    public User(String username, String password, String name, Role role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public boolean isAdmin() {
        return Role.ADMIN.equals(role);
    }

    public boolean isSupervisor() {
        return Role.SUPERVISOR.equals(role);
    }

    public boolean isAuditor() {
        return Role.AUDITOR.equals(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status.equals(UserStatus.ACTIVE);
    }

    public enum UserStatus {
        ACTIVE,
        ON_VACATION,
        ON_SICK_LEAVE,
        INACTIVE
    }

    public enum Role {
        ADMIN,
        SUPERVISOR,
        AUDITOR,
        EMPLOYEE
    }
}
