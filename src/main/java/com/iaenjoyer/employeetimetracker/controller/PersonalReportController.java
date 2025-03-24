package com.iaenjoyer.employeetimetracker.controller;

import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class PersonalReportController {

    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping
    public String showReportForm(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("today", LocalDateTime.now());
        if (user.getRole().equals("ROLE_ADMIN")) {
            return "reports/official_report_form";
        }
        return "reports/personal_report_form";
    }

    @GetMapping("/personal/generate")
    public ResponseEntity<byte[]> generatePersonalReport(
            @AuthenticationPrincipal User user,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");

        byte[] pdfContent = pdfGeneratorService.generatePersonalReport(user, start, end);

        String filename = String.format("informe-personal_%s_%s.pdf",
                start.toLocalDate(),
                end.toLocalDate());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }

    @GetMapping("/employee/generate")
    public ResponseEntity<byte[]> generateEmployeeReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");

        byte[] pdfContent = pdfGeneratorService.generateEmployeeReport(start, end);

        String filename = String.format("informe-empleados_%s_%s.pdf",
                start.toLocalDate(),
                end.toLocalDate());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }
}
