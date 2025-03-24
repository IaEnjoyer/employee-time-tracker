package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.TimeTrackingRule;
import com.iaenjoyer.employeetimetracker.repository.TimeTrackingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TimeTrackingRuleService {
    private final TimeTrackingRuleRepository timeTrackingRuleRepository;

    @Transactional(readOnly = true)
    public List<TimeTrackingRule> findAll() {
        return timeTrackingRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<TimeTrackingRule> findById(Long id) {
        return timeTrackingRuleRepository.findById(id);
    }

    @Transactional
    public TimeTrackingRule save(TimeTrackingRule rule) {
        return timeTrackingRuleRepository.save(rule);
    }

    @Transactional
    public void delete(Long id) {
        timeTrackingRuleRepository.deleteById(id);
    }

    @Transactional
    public TimeTrackingRule createRule(TimeTrackingRule rule) {
        return timeTrackingRuleRepository.save(rule);
    }

    @Transactional
    public TimeTrackingRule updateRule(Long id, TimeTrackingRule updatedRule) {
        return timeTrackingRuleRepository.findById(id)
            .map(rule -> {
                rule.setName(updatedRule.getName());
                rule.setRequireLocation(updatedRule.isRequireLocation());
                rule.setAllowedIps(updatedRule.getAllowedIps());
                rule.setAllowedDevices(updatedRule.getAllowedDevices());
                rule.setAllowManualEntry(updatedRule.isAllowManualEntry());
                rule.setMaxLateMinutes(updatedRule.getMaxLateMinutes());
                rule.setEarlyCheckoutMinutes(updatedRule.getEarlyCheckoutMinutes());
                rule.setRequirePhoto(updatedRule.isRequirePhoto());
                rule.setRequireComment(updatedRule.isRequireComment());
                rule.setNotifyAdmin(updatedRule.isNotifyAdmin());
                rule.setNotifySupervisor(updatedRule.isNotifySupervisor());
                return timeTrackingRuleRepository.save(rule);
            })
            .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada"));
    }

    @Transactional(readOnly = true)
    public TimeTrackingRule getRule(Long id) {
        return timeTrackingRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada"));
    }

    public boolean isIpAllowed(TimeTrackingRule rule, String ip) {
        return rule.getAllowedIps() == null || 
               rule.getAllowedIps().isEmpty() || 
               rule.getAllowedIps().contains(ip);
    }

    public boolean isDeviceAllowed(TimeTrackingRule rule, String deviceId) {
        return rule.getAllowedDevices() == null || 
               rule.getAllowedDevices().isEmpty() || 
               rule.getAllowedDevices().contains(deviceId);
    }

    @Transactional
    public void addAllowedIp(Long ruleId, String ip) {
        TimeTrackingRule rule = getRule(ruleId);
        Set<String> ips = rule.getAllowedIps();
        ips.add(ip);
        rule.setAllowedIps(ips);
        timeTrackingRuleRepository.save(rule);
    }

    @Transactional
    public void removeAllowedIp(Long ruleId, String ip) {
        TimeTrackingRule rule = getRule(ruleId);
        Set<String> ips = rule.getAllowedIps();
        ips.remove(ip);
        rule.setAllowedIps(ips);
        timeTrackingRuleRepository.save(rule);
    }

    @Transactional
    public void addAllowedDevice(Long ruleId, String deviceId) {
        TimeTrackingRule rule = getRule(ruleId);
        Set<String> devices = rule.getAllowedDevices();
        devices.add(deviceId);
        rule.setAllowedDevices(devices);
        timeTrackingRuleRepository.save(rule);
    }

    @Transactional
    public void removeAllowedDevice(Long ruleId, String deviceId) {
        TimeTrackingRule rule = getRule(ruleId);
        Set<String> devices = rule.getAllowedDevices();
        devices.remove(deviceId);
        rule.setAllowedDevices(devices);
        timeTrackingRuleRepository.save(rule);
    }
}
