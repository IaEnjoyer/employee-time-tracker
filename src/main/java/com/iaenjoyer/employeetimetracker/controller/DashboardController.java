package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.TimeRecord;
import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.service.TimeRecordService;
import com.iaenjoyer.employeetimetracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final TimeRecordService timeRecordService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User user, Model model) {
        Optional<TimeRecord> activeRecord = timeRecordService.findActiveRecord(user);
        model.addAttribute("activeRecord", activeRecord.orElse(null));
        model.addAttribute("user", userService.getUser(user.getId())); 
        model.addAttribute("today", LocalDateTime.now());
        return "dashboard";
    }
}
