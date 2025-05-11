package com.iaenjoyer.employeetimetracker.controller;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ui.Model;

import com.iaenjoyer.employeetimetracker.model.TimeRecord;
import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.report.service.JasperFichajesService;
import com.iaenjoyer.employeetimetracker.service.TimeRecordService;
import com.iaenjoyer.employeetimetracker.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/employee/timerecord")
@RequiredArgsConstructor
@Slf4j
public class EmployeeTimeRecordController {
    private final TimeRecordService timeRecordService;
    private final UserService userService;
    @Autowired  
    private final JasperFichajesService jasperFichajesService;

    @PostMapping("/checkin")
    public ResponseEntity<String> checkIn(@AuthenticationPrincipal User currentUser, HttpServletRequest request, Model model) {
        try {
            validateUserForCheckIn(currentUser);
            TimeRecord record = performCheckIn(currentUser, request);
            model.addAttribute("activeRecord", record);
            return ResponseEntity.ok("Checked in");
        } catch (UserAlreadyCheckedInException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Already checked in");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> checkOut(@AuthenticationPrincipal User user, Model model) {
        try {
            timeRecordService.endTimeRecord(user);
            model.addAttribute("activeRecord", null);
            return ResponseEntity.ok("Checked out");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
        }
    }

    private void validateUserForCheckIn(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        // Check if user already has an active time record
        boolean hasActiveTimeRecord = timeRecordService.hasActiveTimeRecord(user);
        if (hasActiveTimeRecord) {
            throw new UserAlreadyCheckedInException("User is already checked in");
        }
    }

    private TimeRecord performCheckIn(User user, HttpServletRequest request) {
        User fullUser = userService.findById(user.getId()).orElseThrow();
        
        String ipAddress = getClientIP(request);
        String deviceInfo = request.getHeader("User-Agent");
    
        log.info("Performing check-in for user: {}", fullUser.getUsername());
        log.info("IP Address: {}, Device Info: {}", ipAddress, deviceInfo);
    
        return timeRecordService.startTimeRecord(fullUser, ipAddress, deviceInfo);
    }
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty() && !"unKnown".equalsIgnoreCase(xfHeader)) {
            // Si hay varios IPs separados por comas, tomar el primero
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    // Custom exception for check-in validation
    private static class UserAlreadyCheckedInException extends RuntimeException {
        public UserAlreadyCheckedInException(String message) {
            super(message);
        }
    }

    @GetMapping("/history")
    public String getTimeRecordHistory(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
        Model model,
        HttpServletRequest request
    ) {
        // If no dates provided, default to current month
        if (start == null || end == null) {
            LocalDateTime now = LocalDateTime.now();
            start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            end = now.plusMonths(1).withDayOfMonth(1).minusNanos(1);
        }
        
        log.debug("Request URL: {}", request.getRequestURL());
        log.debug("Request URI: {}", request.getRequestURI());
        log.debug("Retrieving time records for user: {}", user.getUsername());
        log.debug("Start date: {}, End date: {}", start, end);
        
        List<TimeRecord> records = timeRecordService.findByUserAndStartTimeBetween(user, start, end);
        
        model.addAttribute("timeRecords", records);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        
        log.debug("Returning view: employee/time-record-history");
        return "employee/time-record-history";
    }

    @GetMapping("/report")
    @ResponseBody
    public ResponseEntity<byte[]> generateReport(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        try {
            // If no dates provided, default to current month
            if (start == null || end == null) {
                LocalDateTime now = LocalDateTime.now();
                start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                end = now.plusMonths(1).withDayOfMonth(1).minusNanos(1);
            }
            
            log.debug("Generating report for user: {} from {} to {}", user.getUsername(), start, end);
            
            byte[] report = timeRecordService.generateReport(user, start, end);
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"time-record-report.pdf\"")
                .body(report);
        } catch (Exception e) {
            log.error("Error generating report for user: {}", user.getUsername(), e);
            return ResponseEntity.internalServerError().body(null);
        }
    }
}