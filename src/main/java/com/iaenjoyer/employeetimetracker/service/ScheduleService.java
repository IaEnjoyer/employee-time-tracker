package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.Schedule;
import com.iaenjoyer.employeetimetracker.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Schedule> findById(Long id) {
        return scheduleRepository.findById(id);
    }

    @Transactional
    public Schedule save(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void delete(Long id) {
        scheduleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Schedule> findByDepartment(String department) {
        return scheduleRepository.findByDepartment(department);
    }

    @Transactional(readOnly = true)
    public List<Schedule> findActive() {
        return scheduleRepository.findByActiveTrue();
    }

    @Transactional
    public Schedule activate(Long id) {
        return scheduleRepository.findById(id)
            .map(schedule -> {
                schedule.setActive(true);
                return scheduleRepository.save(schedule);
            })
            .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado"));
    }

    @Transactional
    public Schedule deactivate(Long id) {
        return scheduleRepository.findById(id)
            .map(schedule -> {
                schedule.setActive(false);
                return scheduleRepository.save(schedule);
            })
            .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado"));
    }

    @Transactional(readOnly = true)
    public boolean isWorkingTime(Schedule schedule, LocalTime time) {
        return time.isAfter(schedule.getStartTime()) && 
               time.isBefore(schedule.getEndTime());
    }

    @Transactional(readOnly = true)
    public int calculateWorkingHours(Schedule schedule) {
        int minutes = schedule.getEndTime().toSecondOfDay() / 60 - 
                     schedule.getStartTime().toSecondOfDay() / 60;
        
        if (schedule.getBreakDuration() != null) {
            minutes -= schedule.getBreakDuration();
        }
        
        return minutes / 60;
    }
}
