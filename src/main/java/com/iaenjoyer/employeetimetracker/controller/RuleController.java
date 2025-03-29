package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.TimeTrackingRule;
import com.iaenjoyer.employeetimetracker.service.TimeTrackingRuleService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rules")
@RequiredArgsConstructor
public class RuleController {

    private final TimeTrackingRuleService timeTrackingRuleService;

    @GetMapping
    public String listRules(Model model) {
        model.addAttribute("rules", timeTrackingRuleService.findAll());
        return "admin/rules/list";
    }

    @GetMapping("/new")
    public String newRuleForm(Model model) {
        model.addAttribute("rule", new TimeTrackingRule());
        return "admin/rules/form";
    }

    @PostMapping("/new")
    public String createRule(@ModelAttribute TimeTrackingRule rule, 
                             RedirectAttributes redirectAttributes) {
        try {
            timeTrackingRuleService.save(rule);
            redirectAttributes.addFlashAttribute("successMessage", "Regla creada exitosamente");
            return "redirect:/admin/rules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al crear la regla: " + e.getMessage());
            return "redirect:/admin/rules/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editRuleForm(@PathVariable Long id, Model model) {
        TimeTrackingRule rule = timeTrackingRuleService.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Regla no encontrada"));
        
        model.addAttribute("rule", rule);
        return "admin/rules/form";
    }

    @PutMapping("/{id}")
    public String updateRule(@PathVariable Long id, 
                             @ModelAttribute TimeTrackingRule rule, 
                             RedirectAttributes redirectAttributes) {
        try {
            rule.setId(id);
            timeTrackingRuleService.save(rule);
            redirectAttributes.addFlashAttribute("successMessage", "Regla actualizada exitosamente");
            return "redirect:/admin/rules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al actualizar la regla: " + e.getMessage());
            return "redirect:/admin/rules/" + id + "/edit";
        }
    }

    @DeleteMapping("/{id}")
    public String deleteRule(@PathVariable Long id, 
                             RedirectAttributes redirectAttributes) {
        try {
            timeTrackingRuleService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Regla eliminada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al eliminar la regla: " + e.getMessage());
        }
        return "redirect:/admin/rules";
    }
}