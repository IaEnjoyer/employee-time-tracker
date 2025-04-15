package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private Map<String, Object> settings = new HashMap<>();
    private List<Map<String, Object>> activeAlerts = new ArrayList<>();

    @Transactional(readOnly = true)
    public Map<String, Object> getSettings() {
        return new HashMap<>(settings);
    }

    @Transactional
    public void updateSettings(Map<String, Object> newSettings) {
        settings.clear();
        settings.putAll(newSettings);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }

    public void sendLateArrivalNotification(User user) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "LATE_ARRIVAL");
        alert.put("user", user.getName());
        alert.put("timestamp", System.currentTimeMillis());
        activeAlerts.add(alert);
    }

    public void sendAbsenceNotification(User user) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "ABSENCE");
        alert.put("user", user.getName());
        alert.put("timestamp", System.currentTimeMillis());
        activeAlerts.add(alert);
    }

    public void sendOutsideWorkHoursNotification(User user) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "OUTSIDE_HOURS");
        alert.put("user", user.getName());
        alert.put("timestamp", System.currentTimeMillis());
        activeAlerts.add(alert);
    }

    public void sendApprovalNotification(User user) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "RECORD_APPROVED");
        alert.put("user", user.getName());
        alert.put("timestamp", System.currentTimeMillis());
        activeAlerts.add(alert);
    }

    public void sendRejectionNotification(User user, String reason) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "RECORD_REJECTED");
        alert.put("user", user.getName());
        alert.put("reason", reason);
        alert.put("timestamp", System.currentTimeMillis());
        activeAlerts.add(alert);
    }

    public void sendLeaveRequestNotification(User user, boolean approved) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", approved ? "LEAVE_REQUEST_APPROVED" : "LEAVE_REQUEST_REJECTED");
        alert.put("user", user.getName());
        alert.put("timestamp", System.currentTimeMillis());
        activeAlerts.add(alert);
    }
}
