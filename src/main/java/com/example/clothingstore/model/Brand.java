package com.example.clothingstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "brands")
@Audited
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название бренда обязательно")
    @Size(min = 2, max = 50, message = "Название бренда должно быть от 2 до 50 символов")
    @Column(unique = true)
    private String name;

    @Email(message = "Некорректный email")
    private String contactEmail;

    private boolean deleted = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}