package com.iaenjoyer.employeetimetracker.repository;

import com.iaenjoyer.employeetimetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.iaenjoyer.employeetimetracker.model.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmployeeId(String employeeId);
    List<User> findByDepartment(String department);
    List<User> findByRole(Role role);
    List<User> findByStatus(User.UserStatus status);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
    Optional<User> findByIdAndRole(@Param("id") Long id, @Param("role") Role role);
}
