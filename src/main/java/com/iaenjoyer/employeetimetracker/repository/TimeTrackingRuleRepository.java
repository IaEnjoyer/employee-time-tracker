package com.iaenjoyer.employeetimetracker.repository;

import com.iaenjoyer.employeetimetracker.model.TimeTrackingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeTrackingRuleRepository extends JpaRepository<TimeTrackingRule, Long> {
    // Métodos personalizados si son necesarios
}
