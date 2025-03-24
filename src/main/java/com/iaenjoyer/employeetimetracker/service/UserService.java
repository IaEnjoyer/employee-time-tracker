package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<User> getActiveUsers() {
        return userRepository.findByStatus(User.UserStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersOnVacation() {
        return userRepository.findByStatus(User.UserStatus.ON_VACATION);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersOnSickLeave() {
        return userRepository.findByStatus(User.UserStatus.ON_SICK_LEAVE);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByDepartment(String department) {
        return userRepository.findByDepartment(department);
    }

    @Transactional(readOnly = true)
    public List<User> findByDepartment(String department) {
        return userRepository.findByDepartment(department);
    }

    @Transactional(readOnly = true)
    public Set<String> getAllDepartments() {
        return userRepository.findAll().stream()
                .map(User::getDepartment)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmployeeId(String employeeId) {
        return userRepository.findByEmployeeId(employeeId);
    }

    @Transactional
    public User createUser(User user) {
        validateNewUser(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setHireDate(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public User save(User user) {
        validateNewUser(user);
        return userRepository.save(user);
    }

    @Transactional
    public void delete(User user) {
        userRepository.delete(user);
    }

    @Transactional
    public User updateStatus(User user, User.UserStatus status) {
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public User createUser(String username, String name, String department, User.Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("changeme")); // Contraseña temporal
        user.setName(name);
        user.setDepartment(department);
        user.setRole(role);
        user.setStatus(User.UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!existingUser.getUsername().equals(updatedUser.getUsername()) && 
            userRepository.existsByUsername(updatedUser.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }

        if (!existingUser.getEmail().equals(updatedUser.getEmail()) && 
            userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (updatedUser.getEmployeeId() != null && 
            !updatedUser.getEmployeeId().equals(existingUser.getEmployeeId()) && 
            userRepository.existsByEmployeeId(updatedUser.getEmployeeId())) {
            throw new IllegalArgumentException("El ID de empleado ya está registrado");
        }

        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setDepartment(updatedUser.getDepartment());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setStatus(updatedUser.getStatus());
        existingUser.setSchedule(updatedUser.getSchedule());
        existingUser.setSupervisor(updatedUser.getSupervisor());
        existingUser.setTimeTrackingRule(updatedUser.getTimeTrackingRule());
        existingUser.setEmployeeId(updatedUser.getEmployeeId());
        existingUser.setNotes(updatedUser.getNotes());

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    @Transactional
    public User updateUserStatus(Long id, User.UserStatus status) {
        User user = getUser(id);
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    private void validateNewUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (user.getEmployeeId() != null && userRepository.existsByEmployeeId(user.getEmployeeId())) {
            throw new IllegalArgumentException("El ID de empleado ya está registrado");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (user.getEmail() == null || !user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (user.getDepartment() == null || user.getDepartment().trim().isEmpty()) {
            throw new IllegalArgumentException("El departamento es obligatorio");
        }
    }
}
