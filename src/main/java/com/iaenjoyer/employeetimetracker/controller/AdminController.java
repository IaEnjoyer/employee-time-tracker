package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.*;
import com.iaenjoyer.employeetimetracker.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final TimeRecordService timeRecordService;
    private final NotificationService notificationService;
    private final ReportGeneratorService reportGeneratorService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activeUsers", userService.getActiveUsers());
        model.addAttribute("recentTimeRecords", timeRecordService.findRecentRecords());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", Role.values());
        return "admin/users/list";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        return "admin/users/form";
    }

    @PostMapping("/users/new")
    public String createUser(@ModelAttribute User user, Model model) {
        try {
            user.setStatus(User.UserStatus.ACTIVE);
            user.setEmployeeId(user.getNif());
            user.setConsentDate(LocalDateTime.now(ZoneOffset.UTC)); // Normalización a UTC
            userService.createUser(user);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            logger.warn("Error al crear usuario: {}", e.getMessage());
            model.addAttribute("error", "Error al crear usuario: " + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());
            return "admin/users/form";
        }
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        try {
            userService.findById(id).ifPresentOrElse(
                user -> {
                    model.addAttribute("user", user);
                    model.addAttribute("roles", Role.values());
                },
                () -> logger.error("Usuario no encontrado con ID: {}", id)
            );
            return "admin/users/form";
        } catch (Exception e) {
            logger.error("Error al cargar el formulario de edición del usuario: {}", e.getMessage());
            model.addAttribute("error", "Error al cargar el usuario: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, Model model) {
        try {
            user.setEmployeeId(user.getNif());
            userService.updateUser(id, user);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            logger.warn("Error al actualizar usuario: {}", e.getMessage());
            model.addAttribute("error", "Error al actualizar usuario: " + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());
            return "admin/users/form";
        }
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Usuario eliminado correctamente");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            logger.error("Error al eliminar usuario: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error al eliminar usuario: " + e.getMessage());
            return "redirect:/admin/users";
        } catch (Exception e) {
            logger.error("Error inesperado al eliminar usuario: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error inesperado al eliminar usuario");
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/time-records")
    public String listTimeRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Model model) {
        List<TimeRecord> records;
        if (start != null && end != null) {
            records = timeRecordService.findByDateRange(start, end);
        } else {
            records = timeRecordService.findRecentRecords();
        }
        model.addAttribute("timeRecords", records);
        return "admin/time-records/list";
    }

    @GetMapping("/reports")
    public String showReportForm(Model model) {
        return "admin/reports/form";
    }

    @GetMapping("/reports/preview")
    public String previewReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Model model) {
        try {
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
            }
            Map<String, Object> reportData = reportGeneratorService.generateGeneralReport(start, end);
            model.addAttribute("reportData", reportData);
            return "admin/reports/preview";
        } catch (Exception e) {
            logger.error("Error al generar la vista previa del reporte: {}", e.getMessage());
            model.addAttribute("error", "Error al generar la vista previa: " + e.getMessage());
            return "admin/reports/form";
        }
    }

    @GetMapping("/reports/export/{format}")
    public ResponseEntity<?> exportReport(
            @PathVariable String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
            }
            String filename = String.format("reporte_general_%s", start.format(DATE_FORMATTER));
            byte[] report;
            if ("pdf".equals(format)) {
                report = reportGeneratorService.generateGeneralReportPdf(start, end);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(report);
            } else if ("excel".equals(format)) {
                report = reportGeneratorService.generateGeneralReportExcel(start, end);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".xlsx\"")
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(report);
            } else {
                return ResponseEntity.badRequest().body("Formato de reporte no válido. Formatos permitidos: pdf, excel");
            }
        } catch (Exception e) {
            logger.error("Error al exportar el reporte: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error al generar el reporte: " + e.getMessage());
        }
    }

    @PostMapping("/notifications/settings")
    public ResponseEntity<?> updateNotificationSettings(@RequestBody Map<String, Object> settings) {
        try {
            notificationService.updateSettings(settings);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error al actualizar configuración de notificaciones: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error al actualizar configuración: " + e.getMessage());
        }
    }
}