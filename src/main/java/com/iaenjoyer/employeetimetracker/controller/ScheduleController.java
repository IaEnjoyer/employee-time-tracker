package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.Schedule;
import com.iaenjoyer.employeetimetracker.service.ScheduleService;
import com.iaenjoyer.employeetimetracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String createSchedule(@ModelAttribute Schedule schedule) {
        scheduleService.save(schedule);
        return "redirect:/admin/schedules";
    }

    @GetMapping("/{id}/edit")
    public String editScheduleForm(@PathVariable Long id, Model model) {
        scheduleService.findById(id).ifPresent(schedule -> {
            model.addAttribute("schedule", schedule);
            model.addAttribute("users", userService.findAll());
        });
        return "admin/schedules/form";
    }

    @PostMapping("/{id}")
    public String updateSchedule(@PathVariable Long id, @ModelAttribute Schedule schedule) {
        schedule.setId(id);
        scheduleService.save(schedule);
        return "redirect:/admin/schedules";
    }

    @DeleteMapping("/{id}")
    public String deleteSchedule(@PathVariable Long id) {
        scheduleService.delete(id);
        return "redirect:/admin/schedules";
    }
}
