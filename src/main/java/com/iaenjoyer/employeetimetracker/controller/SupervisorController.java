package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.*;
import com.iaenjoyer.employeetimetracker.model.User.UserStatus;
import com.iaenjoyer.employeetimetracker.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/supervisor")
@PreAuthorize("hasRole('SUPERVISOR')")
@RequiredArgsConstructor
public class SupervisorController {
    private final UserService userService;
    private final ScheduleService scheduleService;

    @GetMapping("/employees")
    public String listEmployees(Model model) {
        model.addAttribute("employees", userService.findEmployeesBySupervisor(userService.getCurrentUser()));
        return "supervisor/employees/list";
    }

    @GetMapping("/employees/new")
    public String newEmployeeForm(Model model) {
        Employee employee = new Employee();
        employee.setSupervisor(userService.getCurrentUser());
        model.addAttribute("employee", employee);
        model.addAttribute("schedules", scheduleService.findAll());
        return "supervisor/employees/form";
    }

    @PostMapping("/employees/new")
    public String createEmployee(@ModelAttribute Employee employee, Model model) {
        try {
            employee.setSupervisor(userService.getCurrentUser());
            employee.setStatus(UserStatus.PENDING_APPROVAL);
            employee.setConsentDate(LocalDateTime.now());
            employee.setStartDate(LocalDateTime.now());
            userService.createEmployee(employee);
            return "redirect:/supervisor/employees";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("employee", employee);
            model.addAttribute("schedules", scheduleService.findAll());
            return "supervisor/employees/form";
        }
    }

    @GetMapping("/employees/{id}/edit")
    public String editEmployeeForm(@PathVariable Long id, Model model) {
        try {
            userService.findEmployeeById(id).ifPresent(employee -> {
                model.addAttribute("employee", employee);
                model.addAttribute("schedules", scheduleService.findAll());
            });
            return "supervisor/employees/form";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el empleado: " + e.getMessage());
            return "redirect:/supervisor/employees";
        }
    }

    @PostMapping("/employees/{id}")
    public String updateEmployee(@PathVariable Long id, @ModelAttribute Employee employee, Model model) {
        try {
            userService.updateEmployee(id, employee);
            return "redirect:/supervisor/employees";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("employee", employee);
            model.addAttribute("schedules", scheduleService.findAll());
            return "supervisor/employees/form";
        }
    }
}
