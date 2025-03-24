package com.iaenjoyer.employeetimetracker.repository;

import com.iaenjoyer.employeetimetracker.model.TimeRecord;
import com.iaenjoyer.employeetimetracker.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeRecordRepository extends JpaRepository<TimeRecord, Long> {
    List<TimeRecord> findByUser(User user);
    List<TimeRecord> findByUserAndStartTimeBetween(User user, LocalDateTime start, LocalDateTime end);
    Optional<TimeRecord> findByUserAndEndTimeIsNull(User user);
    List<TimeRecord> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    List<TimeRecord> findByUserIn(List<User> users);
    List<TimeRecord> findAllByOrderByStartTimeDesc(Pageable pageable);
    List<TimeRecord> findByStatus(TimeRecord.TimeRecordStatus status);
}
