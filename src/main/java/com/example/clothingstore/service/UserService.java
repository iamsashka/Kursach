package com.example.clothingstore.service;

import com.example.clothingstore.model.Role;
import com.example.clothingstore.model.User;
import com.example.clothingstore.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public User save(User user) {
        if (user.getId() != null) {
            User existingUser = userRepository.findById(user.getId()).orElse(null);
            if (existingUser != null) {
                if (!Objects.equals(existingUser.getTheme(), user.getTheme())) {
                    log.info("User {} changed theme from {} to {}",
                            user.getEmail(), existingUser.getTheme(), user.getTheme());
                }
                if (!Objects.equals(existingUser.getPageSize(), user.getPageSize())) {
                    log.info("User {} changed page size from {} to {}",
                            user.getEmail(), existingUser.getPageSize(), user.getPageSize());
                }
            }
        }
        return userRepository.save(user);
    }
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAllByDeletedFalse(pageable);
    }

    public Page<User> getAllCustomers(Pageable pageable) {
        return userRepository.findAllByDeletedFalseAndNotAdmin(pageable);
    }

    public Page<User> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable);
    }

    public Page<User> searchCustomers(String search, Pageable pageable) {
        return userRepository.searchCustomers(search, pageable);
    }
    public User updateProfile(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        existingUser.setUsername(user.getUsername());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setPhone(user.getPhone());
        existingUser.setAddress(user.getAddress());
        existingUser.setCity(user.getCity());
        existingUser.setPostalCode(user.getPostalCode());

        return userRepository.save(existingUser);
    }
    public User saveUser(User user) {
        if (user.getId() == null) {
            if (user.getPassword() != null) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            user.setCreatedAt(LocalDateTime.now());
            user.setEnabled(true);
            user.setDeleted(false);
        } else {
            User existingUser = findById(user.getId());

            if (user.getPassword() == null || user.getPassword().isEmpty() ||
                    user.getPassword().startsWith("$2a$")) {
                user.setPassword(existingUser.getPassword());
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            user.setCreatedAt(existingUser.getCreatedAt());
        }

        user.setLastActivity(LocalDateTime.now());
        return userRepository.save(user);
    }
    public Page<User> getArchivedCustomers(Pageable pageable) {
        return userRepository.findByDeletedTrueOrEnabledFalse(pageable);
    }

    public void restoreCustomer(Long id) {
        User user = findById(id);
        user.setDeleted(false);
        user.setEnabled(true);
        userRepository.save(user);
    }
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndDeletedFalse(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameAndDeletedFalse(username);
    }

    public void softDeleteUser(Long id) {
        User user = findById(id);
        user.setDeleted(true);
        userRepository.save(user);
    }

    public void hardDeleteUser(Long id) {
        userRepository.deleteById(id);
    }
    public User registerUser(User user, Role role) {
        System.out.println("=== DEBUG: UserService.registerUser called ===");
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Email обязателен");
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new RuntimeException("Имя пользователя обязательно");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new RuntimeException("Пароль обязателен");
        }
        userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
            throw new RuntimeException("Пользователь с таким email уже существует");
        });
        userRepository.findByUsername(user.getUsername()).ifPresent(u -> {
            throw new RuntimeException("Пользователь с таким именем уже существует");
        });
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(List.of(role));
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastActivity(LocalDateTime.now());
        user.setDeleted(false);

        System.out.println("=== DEBUG: Saving user to database ===");
        User savedUser = userRepository.save(user);
        System.out.println("=== DEBUG: User saved with ID: " + savedUser.getId());

        return savedUser;
    }

    public User registerCustomer(User user) {
        System.out.println("=== DEBUG: UserService.registerCustomer called ===");
        System.out.println("=== DEBUG: User email: " + user.getEmail() + " ===");
        System.out.println("=== DEBUG: User username: " + user.getUsername() + " ===");

        return registerUser(user, Role.ROLE_CUSTOMER);
    }

    private void validatePassword(String password) {
        if (password.length() < 6) {
            throw new RuntimeException("Пароль должен быть не менее 6 символов");
        }
        if (!password.matches(".*\\d.*")) {
            throw new RuntimeException("Пароль должен содержать хотя бы одну цифру");
        }
        if (!password.matches(".*[A-Za-z].*")) {
            throw new RuntimeException("Пароль должен содержать хотя бы одну букву");
        }
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public void updateLastActivity(String email) {
        User u = userRepository.findByEmail(email).orElse(null);
        if (u != null) {
            u.setLastActivity(LocalDateTime.now());
            userRepository.save(u);
        }
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    public List<User> getAllActiveCustomers() {
        return userRepository.findByEnabledTrueAndDeletedFalse().stream()
                .filter(user -> user.getRoles().contains(Role.ROLE_CUSTOMER))
                .collect(Collectors.toList());
    }

    public List<User> getAllActiveUsers() {
        return userRepository.findByEnabledTrueAndDeletedFalse();
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setEnabled(true);
        userRepository.save(user);
    }

    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setEnabled(false);
        userRepository.save(user);
    }
    public long countAllUsers() {
        return userRepository.count();
    }

    public long countActiveUsers() {
        return userRepository.countByEnabledTrueAndDeletedFalse();
    }

    public long countCustomers() {
        return userRepository.countByRolesContainingAndDeletedFalse(Role.ROLE_CUSTOMER);
    }

    public long countActiveCustomers() {
        return userRepository.countByRolesContainingAndEnabledTrueAndDeletedFalse(Role.ROLE_CUSTOMER);
    }
    public Page<User> searchActiveCustomers(String search, Pageable pageable) {
        return userRepository.searchActiveCustomers(search, pageable);
    }

    public Page<User> searchDeletedCustomers(String search, Pageable pageable) {
        return userRepository.searchDeletedCustomers(search, pageable);
    }
    public Page<User> getRecentlyActiveCustomers(Pageable pageable) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        return userRepository.findRecentlyActiveCustomers(cutoffDate, pageable);
    }
    public Page<User> getAllActiveCustomers(Pageable pageable) {
        return userRepository.findByDeletedFalseAndEnabledTrue(pageable);
    }

    public Page<User> getDeletedCustomers(Pageable pageable) {
        return userRepository.findByDeletedTrue(pageable);
    }
}