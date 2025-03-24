package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.Incident;
import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.service.IncidentService;
import com.iaenjoyer.employeetimetracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;
    private final UserService userService;

    @GetMapping
    public String listIncidents(@AuthenticationPrincipal User user, Model model) {
        List<Incident> assignedIncidents = incidentService.findByAssignee(user);
        List<Incident> reportedIncidents = incidentService.findByReporter(user);
        
        model.addAttribute("assignedIncidents", assignedIncidents);
        model.addAttribute("reportedIncidents", reportedIncidents);
        model.addAttribute("statuses", Incident.IncidentStatus.values());
        return "incidents/list";
    }

    @GetMapping("/new")
    public String showNewIncidentForm(Model model) {
        model.addAttribute("users", userService.findAll());
        return "incidents/form";
    }

    @PostMapping
    public ResponseEntity<?> createIncident(
            @AuthenticationPrincipal User reporter,
            @RequestBody Map<String, String> request) {
        try {
            String description = request.get("description");
            Long assigneeId = Long.parseLong(request.get("assigneeId"));
            
            User assignee = userService.getUser(assigneeId);
            if (assignee == null) {
                return ResponseEntity.badRequest().body("Usuario asignado no encontrado");
            }
            
            Incident incident = incidentService.createIncident(description, reporter, assignee);
            return ResponseEntity.ok(incident);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear el incidente: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public String viewIncident(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        // Buscar en incidentes asignados y reportados
        List<Incident> userIncidents = incidentService.findByAssignee(user);
        userIncidents.addAll(incidentService.findByReporter(user));
        
        Incident incident = userIncidents.stream()
                .filter(i -> i.getId().equals(id))
                .findFirst()
                .orElse(null);
                
        if (incident == null) {
            return "redirect:/incidents";
        }
        
        model.addAttribute("incident", incident);
        model.addAttribute("statuses", Incident.IncidentStatus.values());
        return "incidents/view";
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String resolution = request.get("resolution");
            
            if (!isValidStatusTransition(user, status)) {
                return ResponseEntity.badRequest().body("No tienes permisos para cambiar a este estado");
            }
            
            Incident.IncidentStatus newStatus = Incident.IncidentStatus.valueOf(status.toUpperCase());
            Incident incident = incidentService.updateStatus(id, newStatus, resolution);
            
            return ResponseEntity.ok(incident);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar el estado: " + e.getMessage());
        }
    }

    private boolean isValidStatusTransition(User user, String status) {
        Incident.IncidentStatus newStatus = Incident.IncidentStatus.valueOf(status.toUpperCase());
        
        // Solo los administradores pueden cerrar o rechazar incidentes
        if ((newStatus == Incident.IncidentStatus.CLOSED || 
             newStatus == Incident.IncidentStatus.REJECTED) && 
            !user.isAdmin()) {
            return false;
        }
        
        // Cualquier usuario puede marcar como en progreso o resuelto
        return true;
    }
}
