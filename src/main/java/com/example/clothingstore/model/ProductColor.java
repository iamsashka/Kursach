package com.example.clothingstore.model;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "product_colors")
@Audited
public class ProductColor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String hexCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    public ProductColor() {}

    public ProductColor(String name, String hexCode, Product product) {
        this.name = name;
        this.hexCode = hexCode;
        this.product = product;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHexCode() { return hexCode; }
    public void setHexCode(String hexCode) { this.hexCode = hexCode; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}