package com.iaenjoyer.employeetimetracker.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iaenjoyer.employeetimetracker.model.Role;
import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.repository.TimeRecordRepository;
import com.iaenjoyer.employeetimetracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimeRecordRepository timeRecordRepository;

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
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
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
    public User createUser(String username, String name, String department, Role role) {
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
        existingUser.setNif(updatedUser.getEmployeeId());
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

    @Transactional
    public User createEmployee(User employee) {
        validateNewUser(employee);
        if (employee.getPassword() != null) {
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        }
        return userRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public Optional<User> findEmployeeById(Long id) {
        return userRepository.findByIdAndRole(id, Role.EMPLOYEE)
                .filter(user -> user instanceof User)
                .map(user -> user);
    }

    @Transactional
    public User updateEmployee(Long id, User employee) {
        User existingEmployee = findEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado"));

        if (!existingEmployee.getId().equals(employee.getId())) {
            throw new IllegalArgumentException("ID de empleado no coincide");
        }

        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        } else {
            employee.setPassword(existingEmployee.getPassword());
        }

        return userRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        return findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario no autenticado"));
    }

    @Transactional
    public void deleteUser(Long id) {
        // Buscar el usuario directamente en el repositorio
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Verificaciones previas a la eliminación
        if (timeRecordRepository.existsByUser(user)) {
            throw new IllegalArgumentException("No se puede eliminar un usuario con registros de tiempo");
        }

        // Eliminar el usuario directamente
        userRepository.deleteById(id);
        userRepository.flush();
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
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }
        if (user.getEmployeeId() == null || user.getEmployeeId().trim().isEmpty()) {
            throw new IllegalArgumentException("El documento de identificación es obligatorio");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
    }
}
