package com.iaenjoyer.employeetimetracker.config;

import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            createDefaultUsers();
        }
    }

    private void createDefaultUsers() {
        LocalDateTime now = LocalDateTime.now();

        // Admin
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setEmail("admin@company.com");
        admin.setName("Administrador");
        admin.setRole(User.Role.ADMIN);
        admin.setStatus(User.UserStatus.ACTIVE);
        admin.setDepartment("Administración");
        admin.setDataConsent(true);
        admin.setConsentDate(now);
        admin.setEmployeeId("EMP001");
        admin.setHireDate(now);
        userRepository.save(admin);

        // Supervisor
        User supervisor = new User();
        supervisor.setUsername("supervisor");
        supervisor.setPassword(passwordEncoder.encode("supervisor"));
        supervisor.setEmail("supervisor@company.com");
        supervisor.setName("Supervisor");
        supervisor.setRole(User.Role.SUPERVISOR);
        supervisor.setStatus(User.UserStatus.ACTIVE);
        supervisor.setDepartment("Operaciones");
        supervisor.setDataConsent(true);
        supervisor.setConsentDate(now);
        supervisor.setEmployeeId("EMP002");
        supervisor.setHireDate(now);
        userRepository.save(supervisor);

        // Employee
        User employee = new User();
        employee.setUsername("employee");
        employee.setPassword(passwordEncoder.encode("employee"));
        employee.setEmail("employee@company.com");
        employee.setName("Empleado");
        employee.setRole(User.Role.EMPLOYEE);
        employee.setStatus(User.UserStatus.ACTIVE);
        employee.setDepartment("Ventas");
        employee.setDataConsent(true);
        employee.setConsentDate(now);
        employee.setEmployeeId("EMP003");
        employee.setHireDate(now);
        employee.setSupervisor(supervisor);
        userRepository.save(employee);

        // Auditor
        User auditor = new User();
        auditor.setUsername("auditor");
        auditor.setPassword(passwordEncoder.encode("auditor"));
        auditor.setEmail("auditor@company.com");
        auditor.setName("Auditor");
        auditor.setRole(User.Role.AUDITOR);
        auditor.setStatus(User.UserStatus.ACTIVE);
        auditor.setDepartment("Auditoría");
        auditor.setDataConsent(true);
        auditor.setConsentDate(now);
        auditor.setEmployeeId("EMP004");
        auditor.setHireDate(now);
        userRepository.save(auditor);
    }
}
