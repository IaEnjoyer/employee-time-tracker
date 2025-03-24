package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.*;
import com.iaenjoyer.employeetimetracker.service.*;
import com.iaenjoyer.employeetimetracker.service.pdf.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final TimeRecordService timeRecordService;
    private final IncidentService incidentService;
    private final PdfGeneratorService pdfGeneratorService;
    private final TimeTrackingRuleService timeTrackingRuleService;
    private final NotificationService notificationService;
    private final ScheduleService scheduleService;
    private final ReportGeneratorService reportGeneratorService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activeUsers", userService.getActiveUsers());
        model.addAttribute("usersOnVacation", userService.getUsersOnVacation());
        model.addAttribute("usersOnSickLeave", userService.getUsersOnSickLeave());
        model.addAttribute("pendingIncidents", incidentService.findByStatus(Incident.IncidentStatus.OPEN));
        model.addAttribute("recentTimeRecords", timeRecordService.findRecentRecords());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("departments", userService.getAllDepartments());
        model.addAttribute("roles", Role.values());
        model.addAttribute("schedules", scheduleService.findAll());
        model.addAttribute("supervisors", userService.getUsersByRole(Role.SUPERVISOR));
        return "admin/users/list";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        model.addAttribute("schedules", scheduleService.findAll());
        model.addAttribute("supervisors", userService.getUsersByRole(Role.SUPERVISOR));
        return "admin/users/form";
    }

    @PostMapping("/users/new")
    public String createUser(@ModelAttribute User user, Model model) {
        try {
            user.setStatus(User.UserStatus.ACTIVE);
            user.setConsentDate(LocalDateTime.now());
            userService.createUser(user);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());
            model.addAttribute("schedules", scheduleService.findAll());
            model.addAttribute("supervisors", userService.getUsersByRole(Role.SUPERVISOR));
            return "admin/users/form";
        }
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        try {
            userService.findById(id).ifPresent(user -> {
                model.addAttribute("user", user);
                model.addAttribute("roles", Role.values());
                model.addAttribute("schedules", scheduleService.findAll());
                model.addAttribute("supervisors", userService.getUsersByRole(Role.SUPERVISOR));
            });
            return "admin/users/form";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el usuario: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, Model model) {
        try {
            userService.updateUser(id, user);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());
            model.addAttribute("schedules", scheduleService.findAll());
            model.addAttribute("supervisors", userService.getUsersByRole(Role.SUPERVISOR));
            return "admin/users/form";
        }
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, Model model) {
        try {
            userService.deactivateUser(id);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/{id}/activate")
    public String activateUser(@PathVariable Long id, Model model) {
        try {
            userService.activateUser(id);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Usuario eliminado correctamente");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            // Loguear el error para depuración
            System.err.println("Error al eliminar usuario: " + e.getMessage());
            
            // Añadir mensaje de error específico
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        } catch (Exception e) {
            // Capturar cualquier otra excepción inesperada
            System.err.println("Error inesperado al eliminar usuario: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error inesperado al eliminar usuario");
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/time-records")
    public String listTimeRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String department,
            Model model) {
        
        List<TimeRecord> records;
        if (start != null && end != null) {
            records = timeRecordService.findByDateRange(start, end);
        } else if (department != null) {
            records = timeRecordService.findByDepartment(department);
        } else {
            records = timeRecordService.findRecentRecords();
        }
        
        model.addAttribute("timeRecords", records);
        model.addAttribute("departments", userService.getAllDepartments());
        return "admin/time-records/list";
    }

    @PostMapping("/time-records/{id}/approve")
    public ResponseEntity<?> approveTimeRecord(@PathVariable Long id) {
        try {
            TimeRecord record = timeRecordService.approveTimeRecord(id);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al aprobar registro: " + e.getMessage());
        }
    }

    @PostMapping("/time-records/{id}/reject")
    public ResponseEntity<?> rejectTimeRecord(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            TimeRecord record = timeRecordService.rejectTimeRecord(id, reason);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al rechazar registro: " + e.getMessage());
        }
    }

    @GetMapping("/incidents")
    public String listIncidents(Model model) {
        model.addAttribute("incidents", incidentService.findByStatus(Incident.IncidentStatus.OPEN));
        return "admin/incidents/list";
    }

    @GetMapping("/incidents/new")
    public String showIncidentForm(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("incident", new Incident());
        model.addAttribute("incidentTypes", Incident.IncidentType.values());
        return "admin/incidents/form";
    }

    @PostMapping("/incidents/new")
    public String createIncident(@ModelAttribute Incident incident, 
                                 @RequestParam("reporterId") Long reporterId, 
                                 Model model) {
        try {
            // Get the current user as the reporter
            User reporter = userService.getUser(reporterId);
            
            if (reporter == null) {
                model.addAttribute("error", "Usuario no encontrado");
                model.addAttribute("users", userService.findAll());
                model.addAttribute("incidentTypes", Incident.IncidentType.values());
                return "admin/incidents/form";
            }

            // Set the reporter
            incident.setReporter(reporter);

            // Create the incident
            Incident createdIncident = incidentService.createIncident(
                incident.getDescription(), 
                reporter, 
                incident.getAssignee(), 
                incident.getType()
            );

            return "redirect:/admin/incidents";
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear el incidente: " + e.getMessage());
            model.addAttribute("users", userService.findAll());
            model.addAttribute("incidentTypes", Incident.IncidentType.values());
            return "admin/incidents/form";
        }
    }

    @GetMapping("/incidents/{id}/edit")
    public String editIncidentForm(@PathVariable Long id, Model model) {
        Incident incident = incidentService.findById(id);
        model.addAttribute("incident", incident);
        model.addAttribute("users", userService.findAll());
        model.addAttribute("incidentTypes", Incident.IncidentType.values());
        return "admin/incidents/form";
    }

    @PostMapping("/incidents/{id}")
    public String updateIncident(@PathVariable Long id, 
                                 @ModelAttribute Incident incident, 
                                 @RequestParam("reporterId") Long reporterId, 
                                 Model model) {
        try {
            // Get the current user as the reporter
            User reporter = userService.getUser(reporterId);
            
            if (reporter == null) {
                model.addAttribute("error", "Usuario no encontrado");
                model.addAttribute("users", userService.findAll());
                model.addAttribute("incidentTypes", Incident.IncidentType.values());
                return "admin/incidents/form";
            }

            // Ensure the incident has the correct ID
            incident.setId(id);

            // Update the incident
            Incident updatedIncident = incidentService.updateIncident(id, incident);

            return "redirect:/admin/incidents";
        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar el incidente: " + e.getMessage());
            model.addAttribute("users", userService.findAll());
            model.addAttribute("incidentTypes", Incident.IncidentType.values());
            return "admin/incidents/form";
        }
    }

    @PostMapping("/incidents/{id}/approve")
    public String approveIncident(@PathVariable Long id, Model model) {
        try {
            Incident incident = incidentService.updateStatus(id, Incident.IncidentStatus.IN_PROGRESS, null);
            return "redirect:/admin/incidents";
        } catch (Exception e) {
            model.addAttribute("error", "Error al aprobar la incidencia: " + e.getMessage());
            return "redirect:/admin/incidents";
        }
    }

    @PostMapping("/incidents/{id}/reject")
    public String rejectIncident(@PathVariable Long id, 
                                 @RequestParam("reason") String reason, 
                                 Model model) {
        try {
            Incident incident = incidentService.updateStatus(id, Incident.IncidentStatus.REJECTED, reason);
            return "redirect:/admin/incidents";
        } catch (Exception e) {
            model.addAttribute("error", "Error al rechazar la incidencia: " + e.getMessage());
            return "redirect:/admin/incidents";
        }
    }

    @PostMapping("/incidents/{id}/status")
    public ResponseEntity<?> updateIncidentStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String resolution = request.get("resolution");
            
            Incident.IncidentStatus newStatus = Incident.IncidentStatus.valueOf(status.toUpperCase());
            Incident incident = incidentService.updateStatus(id, newStatus, resolution);
            
            return ResponseEntity.ok(incident);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar el estado: " + e.getMessage());
        }
    }

    @GetMapping("/reports/department/{department}")
    public ResponseEntity<byte[]> generateDepartmentReport(
            @PathVariable String department,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            Map<String, Object> reportData = reportGeneratorService.generateDepartmentReport(department, start, end);
            byte[] pdf = pdfGeneratorService.generatePdf(reportData);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\"")
                    .contentType(MediaType.parseMediaType("application/pdf"))
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/notifications/settings")
    public String notificationSettings(Model model) {
        model.addAttribute("settings", notificationService.getSettings());
        model.addAttribute("alerts", notificationService.getActiveAlerts());
        return "admin/notifications/settings";
    }

    @PostMapping("/notifications/settings")
    public ResponseEntity<?> updateNotificationSettings(@RequestBody Map<String, Object> settings) {
        try {
            notificationService.updateSettings(settings);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar configuración: " + e.getMessage());
        }
    }

    @GetMapping("/reports")
    public String showReportForm(Model model) {
        model.addAttribute("departments", List.of("Ventas", "Marketing", "IT", "RRHH"));
        return "admin/reports/form";
    }

    @GetMapping("/reports/generate")
    @ResponseBody
    public ResponseEntity<?> generateReport(
            @RequestParam String type,
            @RequestParam(required = false) String department,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        try {
            Map<String, Object> report;
            if ("department".equals(type)) {
                if (department == null || department.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("El departamento es obligatorio para reportes por departamento");
                }
                report = reportGeneratorService.generateDepartmentReport(department, start, end);
            } else if ("general".equals(type)) {
                report = reportGeneratorService.generateGeneralReport(start, end);
            } else {
                return ResponseEntity.badRequest().body("Tipo de reporte no válido");
            }
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al generar el reporte: " + e.getMessage());
        }
    }

    @GetMapping("/reports/download")
    public ResponseEntity<?> downloadReport(
            @RequestParam String type,
            @RequestParam(required = false) String department,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        try {
            byte[] report;
            if ("department".equals(type)) {
                if (department == null || department.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("El departamento es obligatorio para reportes por departamento");
                }
                report = reportGeneratorService.generateOfficialDepartmentReport(department, start, end);
            } else if ("general".equals(type)) {
                report = reportGeneratorService.generateOfficialReport(start, end);
            } else {
                return ResponseEntity.badRequest().body("Tipo de reporte no válido");
            }

            String filename = String.format("reporte_%s_%s%s.xlsx",
                    type,
                    department != null ? department + "_" : "",
                    start.format(DATE_FORMATTER));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al generar el reporte: " + e.getMessage());
        }
    }

    @GetMapping("/reports/export/pdf")
    public ResponseEntity<?> exportReportPdf(
            @RequestParam String type,
            @RequestParam(required = false) String department,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        try {
            byte[] report;
            if ("department".equals(type)) {
                if (department == null || department.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("El departamento es obligatorio para reportes por departamento");
                }
                report = reportGeneratorService.generateDepartmentReportPdf(department, start, end);
            } else if ("general".equals(type)) {
                report = reportGeneratorService.generateGeneralReportPdf(start, end);
            } else {
                return ResponseEntity.badRequest().body("Tipo de reporte no válido");
            }

            String filename = String.format("reporte_%s_%s%s.pdf",
                    type,
                    department != null ? department + "_" : "",
                    start.format(DATE_FORMATTER));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al generar el reporte PDF: " + e.getMessage());
        }
    }

    @GetMapping("/reports/export/excel")
    public ResponseEntity<?> exportReportExcel(
            @RequestParam String type,
            @RequestParam(required = false) String department,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        try {
            byte[] report;
            if ("department".equals(type)) {
                if (department == null || department.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("El departamento es obligatorio para reportes por departamento");
                }
                report = reportGeneratorService.generateDepartmentReportExcel(department, start, end);
            } else if ("general".equals(type)) {
                report = reportGeneratorService.generateGeneralReportExcel(start, end);
            } else {
                return ResponseEntity.badRequest().body("Tipo de reporte no válido");
            }

            String filename = String.format("reporte_%s_%s%s.xlsx",
                    type,
                    department != null ? department + "_" : "",
                    start.format(DATE_FORMATTER));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al generar el reporte Excel: " + e.getMessage());
        }
    }

    @GetMapping("/rules")
    public String listRules(Model model) {
        model.addAttribute("rules", timeTrackingRuleService.findAll());
        return "admin/rules/list";
    }

    @GetMapping("/rules/new")
    public String newRuleForm(Model model) {
        model.addAttribute("rule", new TimeTrackingRule());
        return "admin/rules/form";
    }

    @PostMapping("/rules/new")
    public String createRule(@ModelAttribute TimeTrackingRule rule, Model model) {
        try {
            timeTrackingRuleService.save(rule);
            return "redirect:/admin/rules";
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear la regla: " + e.getMessage());
            return "admin/rules/form";
        }
    }

    @GetMapping("/rules/{id}/edit")
    public String editRuleForm(@PathVariable Long id, Model model) {
        try {
            timeTrackingRuleService.findById(id).ifPresent(rule -> {
                model.addAttribute("rule", rule);
            });
            return "admin/rules/form";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la regla: " + e.getMessage());
            return "redirect:/admin/rules";
        }
    }

    @PostMapping("/rules/{id}")
    public String updateRule(@PathVariable Long id, @ModelAttribute TimeTrackingRule rule, Model model) {
        try {
            rule.setId(id);
            timeTrackingRuleService.save(rule);
            return "redirect:/admin/rules";
        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar la regla: " + e.getMessage());
            return "admin/rules/form";
        }
    }
}
