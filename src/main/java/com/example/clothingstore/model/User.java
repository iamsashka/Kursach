package com.example.clothingstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Audited
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
    private String username;
    @Transient
    private String role;
    @Column(nullable = false)
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private List<Role> roles = new ArrayList<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
    private String firstName;

    @Size(min = 2, max = 50, message = "Фамилия должна быть от 2 до 50 символов")
    private String lastName;

    @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Некорректный формат телефона")
    private String phone;

    private String address;
    private String city;
    private String postalCode;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastActivity;

    private boolean deleted = false;


    public User() {
    }

    public User(String email, String username, String password, String firstName, String lastName) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = true;
    }
    @Column(name = "user_theme")
    private String theme = "light";

    @Column(name = "date_format")
    private String dateFormat = "dd.MM.yyyy";

    @Column(name = "number_format")
    private String numberFormat = "COMMA";

    @Column(name = "page_size")
    private Integer pageSize = 10;

    @Column(name = "saved_filters", columnDefinition = "TEXT")
    private String savedFilters;

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public String getNumberFormat() { return numberFormat; }
    public void setNumberFormat(String numberFormat) { this.numberFormat = numberFormat; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getSavedFilters() { return savedFilters; }
    public void setSavedFilters(String savedFilters) { this.savedFilters = savedFilters; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<Role> getRoles() { return roles; }
    public void setRoles(List<Role> roles) { this.roles = roles; }

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new ArrayList<>();
        }
        this.roles.add(role);
    }


    public boolean hasRole(Role role) {
        return this.roles != null && this.roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(Role.ROLE_ADMIN);
    }
    public boolean isManager() {
        return hasRole(Role.ROLE_MANAGER);
    }
    public boolean isCustomer() {
        return hasRole(Role.ROLE_CUSTOMER);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", enabled=" + enabled +
                ", roles=" + roles +
                ", createdAt=" + createdAt +
                '}';
    }
}