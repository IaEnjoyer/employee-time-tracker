package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.Role;
import com.iaenjoyer.employeetimetracker.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        User user = (User) auth.getPrincipal();
        if (Role.ADMIN.equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
