package com.iaenjoyer.employeetimetracker.repository;

import com.iaenjoyer.employeetimetracker.model.Incident;
import com.iaenjoyer.employeetimetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByAssignee(User assignee);
    
    List<Incident> findByReporter(User reporter);
    
    List<Incident> findByStatus(Incident.IncidentStatus status);
    
    List<Incident> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Incident> findByAssigneeAndStatus(User assignee, Incident.IncidentStatus status);
    
    List<Incident> findByReporterAndStatus(User reporter, Incident.IncidentStatus status);

    boolean existsByReporter(User reporter);
    
    boolean existsByAssignee(User assignee);
}
