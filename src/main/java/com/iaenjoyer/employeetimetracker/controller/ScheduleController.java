package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.Schedule;
import com.iaenjoyer.employeetimetracker.service.ScheduleService;
import com.iaenjoyer.employeetimetracker.service.UserService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserService userService;

    @GetMapping
    public String listSchedules(Model model) {
        model.addAttribute("schedules", scheduleService.findAll());
        return "admin/schedules/list";
    }

    @GetMapping("/new")
    public String newScheduleForm(Model model) {
        model.addAttribute("schedule", new Schedule());
        model.addAttribute("users", userService.findAll());
        return "admin/schedules/form";
    }

    @PostMapping
    public String createSchedule(@ModelAttribute Schedule schedule,
            RedirectAttributes redirectAttributes) {
        try {
            scheduleService.save(schedule);
            redirectAttributes.addFlashAttribute("successMessage", "Horario creado exitosamente");
            return "redirect:/admin/schedules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al crear el horario: " + e.getMessage());
            return "redirect:/admin/schedules/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editScheduleForm(@PathVariable Long id, Model model) {
        Schedule schedule = scheduleService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Horario no encontrado"));
        model.addAttribute("schedule", schedule);
        model.addAttribute("users", userService.findAll());
        return "admin/schedules/form";
    }

    @PutMapping("/{id}")
    public String updateSchedule(@PathVariable Long id,
            @ModelAttribute Schedule schedule,
            RedirectAttributes redirectAttributes) {
        try {
            schedule.setId(id);
            scheduleService.save(schedule);
            redirectAttributes.addFlashAttribute("successMessage", "Horario actualizado exitosamente");
            return "redirect:/admin/schedules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al actualizar el horario: " + e.getMessage());
            return "redirect:/admin/schedules/" + id + "/edit";
        }
    }

    @DeleteMapping("/{id}")
    public String deleteSchedule(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            scheduleService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Horario eliminado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al eliminar el horario: " + e.getMessage());
        }
        return "redirect:/admin/schedules";
    }

}
