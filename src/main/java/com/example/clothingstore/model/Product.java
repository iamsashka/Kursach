package com.example.clothingstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Audited
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название товара обязательно")
    @Size(min = 2, max = 200, message = "Название должно быть от 2 до 200 символов")
    private String name;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    private BigDecimal price;

    @Min(value = 0, message = "Количество не может быть отрицательным")
    private Integer stockQuantity;

    private String sizes;

    // Связь с изображениями
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    // Связь с цветами
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductColor> colors = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @NotNull(message = "Категория обязательна")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @NotNull(message = "Бренд обязателен")
    private Brand brand;

    private boolean deleted = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private Set<ProductTag> tags = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience")
    private TargetAudience targetAudience;

    @Column(name = "country_of_origin")
    private String countryOfOrigin;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    private Integer views = 0;

    private Double rating = 0.0;

    private Integer reviewCount = 0;

    public Product() {}

    public Product(String name, String description, BigDecimal price, Integer stockQuantity,
                   Category category, Brand brand) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.brand = brand;
        this.createdAt = LocalDateTime.now();
    }
    @Transient
    private Boolean isInFavorites = false;

    public Boolean getIsInFavorites() {
        return isInFavorites;
    }

    public void setIsInFavorites(Boolean inFavorites) {
        isInFavorites = inFavorites;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getSizes() { return sizes; }
    public void setSizes(String sizes) { this.sizes = sizes; }

    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }

    public List<ProductColor> getColors() { return colors; }
    public void setColors(List<ProductColor> colors) { this.colors = colors; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public Set<ProductTag> getTags() { return tags; }
    public void setTags(Set<ProductTag> tags) { this.tags = tags; }

    public TargetAudience getTargetAudience() { return targetAudience; }
    public void setTargetAudience(TargetAudience targetAudience) { this.targetAudience = targetAudience; }

    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public boolean isOnSale() {
        return originalPrice != null && price != null &&
                originalPrice.compareTo(BigDecimal.ZERO) > 0 &&
                originalPrice.compareTo(price) > 0;
    }

    public BigDecimal getDiscountPercent() {
        if (isOnSale()) {
            BigDecimal discount = originalPrice.subtract(price);
            return discount.divide(originalPrice, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getDiscountAmount() {
        if (isOnSale()) {
            return originalPrice.subtract(price);
        }
        return BigDecimal.ZERO;
    }

    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    public boolean isLowStock() {
        return stockQuantity != null && stockQuantity > 0 && stockQuantity <= 5;
    }

    public List<String> getAvailableSizes() {
        if (sizes == null || sizes.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> sizeList = new ArrayList<>();
        String[] sizeArray = sizes.split(",");
        for (String size : sizeArray) {
            sizeList.add(size.trim());
        }
        return sizeList;
    }

    public List<String> getAvailableColorNames() {
        if (colors == null || colors.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> colorNames = new ArrayList<>();
        for (ProductColor color : colors) {
            colorNames.add(color.getName());
        }
        return colorNames;
    }

    public boolean hasTag(ProductTag tag) {
        return tags != null && tags.contains(tag);
    }

    public void addTag(ProductTag tag) {
        if (this.tags == null) {
            this.tags = new HashSet<>();
        }
        this.tags.add(tag);
    }

    public void removeTag(ProductTag tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
        }
    }

    public String getMainImage() {
        if (images == null || images.isEmpty()) {
            return "";
        }
        return images.stream()
                .filter(ProductImage::isMain)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(images.get(0).getImageUrl());
    }

    public void addImage(ProductImage image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        image.setProduct(this);
        this.images.add(image);
    }

    public void addColor(ProductColor color) {
        if (this.colors == null) {
            this.colors = new ArrayList<>();
        }
        color.setProduct(this);
        this.colors.add(color);
    }

    public void incrementViews() {
        if (this.views == null) {
            this.views = 0;
        }
        this.views++;
    }

    public void updateRating(Double newRating) {
        if (this.rating == null) {
            this.rating = 0.0;
        }
        if (this.reviewCount == null) {
            this.reviewCount = 0;
        }

        double totalScore = this.rating * this.reviewCount + newRating;
        this.reviewCount++;
        this.rating = totalScore / this.reviewCount;
    }

    public boolean isNewArrival() {
        if (createdAt == null) {
            return false;
        }
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return createdAt.isAfter(thirtyDaysAgo);
    }

    public boolean decreaseStock(Integer quantity) {
        if (stockQuantity == null || stockQuantity < quantity) {
            return false;
        }
        this.stockQuantity -= quantity;
        return true;
    }

    public void increaseStock(Integer quantity) {
        if (this.stockQuantity == null) {
            this.stockQuantity = 0;
        }
        this.stockQuantity += quantity;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                ", brand=" + (brand != null ? brand.getName() : "null") +
                ", category=" + (category != null ? category.getName() : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}