package com.iaenjoyer.employeetimetracker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "employees")
@PrimaryKeyJoinColumn(name = "user_id")
public class Employee extends User {
    
    @Column(name = "performance_rating")
    private Integer performanceRating;
    
    @Column(name = "skills", length = 1000)
    private String skills;
    
    @Column(name = "certifications", length = 1000)
    private String certifications;
    
    @Column(name = "emergency_contact", length = 500)
    private String emergencyContact;
    
    @Column(name = "position", length = 100)
    private String position;
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "salary")
    private Double salary;
    
    @Column(name = "contract_type", length = 50)
    private String contractType;
    
    @Column(name = "work_schedule", length = 200)
    private String workSchedule;
    
    @Column(name = "vacation_days")
    private Integer vacationDays;
    
    @Column(name = "sick_leave_days")
    private Integer sickLeaveDays;
    
    public Employee(String username, String password, String name) {
        super(username, password, name, Role.EMPLOYEE);
        this.startDate = LocalDateTime.now();
        this.vacationDays = 22; // Días de vacaciones por defecto según la ley española
        this.sickLeaveDays = 0;
    }
}
