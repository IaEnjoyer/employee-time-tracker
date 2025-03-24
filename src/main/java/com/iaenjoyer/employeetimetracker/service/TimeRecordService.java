package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.TimeRecord;
import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.repository.TimeRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TimeRecordService {
    private static final int RECENT_RECORDS_LIMIT = 10;
    
    private final TimeRecordRepository timeRecordRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<TimeRecord> findByUserAndStartTimeBetween(User user, LocalDateTime start, LocalDateTime end) {
        return timeRecordRepository.findByUserAndStartTimeBetween(user, start, end);
    }

    @Transactional(readOnly = true)
    public Optional<TimeRecord> findActiveRecord(User user) {
        return timeRecordRepository.findByUserAndEndTimeIsNull(user);
    }

    @Transactional(readOnly = true)
    public List<TimeRecord> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return timeRecordRepository.findByStartTimeBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<TimeRecord> findByDepartment(String department) {
        List<User> departmentUsers = userService.findByDepartment(department);
        return timeRecordRepository.findByUserIn(departmentUsers);
    }

    @Transactional(readOnly = true)
    public List<TimeRecord> findRecentRecords() {
        return timeRecordRepository.findAllByOrderByStartTimeDesc(PageRequest.of(0, RECENT_RECORDS_LIMIT));
    }

    @Transactional(readOnly = true)
    public List<TimeRecord> findByDepartmentAndDateRange(String department, LocalDateTime start, LocalDateTime end) {
        return timeRecordRepository.findByUserDepartmentAndStartTimeBetweenOrderByStartTimeDesc(department, start, end);
    }

    @Transactional
    public TimeRecord approveTimeRecord(Long id) {
        TimeRecord record = timeRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado"));
        
        record.setStatus(TimeRecord.Status.APPROVED);
        notificationService.sendApprovalNotification(record.getUser());
        return timeRecordRepository.save(record);
    }

    @Transactional
    public TimeRecord rejectTimeRecord(Long id, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Se requiere una razón para el rechazo");
        }

        TimeRecord record = timeRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado"));
        
        record.setStatus(TimeRecord.Status.REJECTED);
        record.setRejectionReason(reason);
        notificationService.sendRejectionNotification(record.getUser(), reason);
        return timeRecordRepository.save(record);
    }

    @Transactional
    public TimeRecord startTimeRecord(User user) {
        // Verificar si ya existe un registro activo
        Optional<TimeRecord> activeRecord = findActiveRecord(user);
        if (activeRecord.isPresent()) {
            throw new IllegalStateException("Ya existe un registro activo para este usuario");
        }

        TimeRecord record = new TimeRecord();
        record.setUser(user);
        record.setStartTime(LocalDateTime.now());
        record.setStatus(TimeRecord.Status.PENDING);
        return timeRecordRepository.save(record);
    }

    @Transactional
    public TimeRecord endTimeRecord(User user) {
        TimeRecord record = findActiveRecord(user)
                .orElseThrow(() -> new IllegalStateException("No hay un registro activo para este usuario"));
        
        record.setEndTime(LocalDateTime.now());
        return timeRecordRepository.save(record);
    }
}
