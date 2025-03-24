package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.Incident;
import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentRepository incidentRepository;

    @Transactional
    public Incident createIncident(String description, User reporter, User assignee) {
        Incident incident = new Incident();
        incident.setDescription(description);
        incident.setReporter(reporter);
        incident.setAssignee(assignee);
        incident.setStatus(Incident.IncidentStatus.OPEN);
        incident.setCreatedAt(LocalDateTime.now());
        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public List<Incident> findByAssignee(User assignee) {
        return incidentRepository.findByAssignee(assignee);
    }

    @Transactional(readOnly = true)
    public List<Incident> findByReporter(User reporter) {
        return incidentRepository.findByReporter(reporter);
    }

    @Transactional(readOnly = true)
    public List<Incident> findByStatus(Incident.IncidentStatus status) {
        return incidentRepository.findByStatus(status);
    }

    @Transactional
    public Incident updateStatus(Long incidentId, Incident.IncidentStatus newStatus, String resolution) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidente no encontrado"));
        
        incident.setStatus(newStatus);
        incident.setResolution(resolution);
        if (newStatus == Incident.IncidentStatus.RESOLVED || newStatus == Incident.IncidentStatus.CLOSED) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        
        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public List<Incident> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return incidentRepository.findByCreatedAtBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<Incident> findByAssigneeAndStatus(User assignee, Incident.IncidentStatus status) {
        return incidentRepository.findByAssigneeAndStatus(assignee, status);
    }

    @Transactional(readOnly = true)
    public List<Incident> findByReporterAndStatus(User reporter, Incident.IncidentStatus status) {
        return incidentRepository.findByReporterAndStatus(reporter, status);
    }
}
