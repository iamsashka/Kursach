package com.example.clothingstore.controller.rest;

import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.ProductTag;
import com.example.clothingstore.model.TargetAudience;
import com.example.clothingstore.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products API", description = "API для управления товарами")
public class ProductApiController {

    private final ProductService productService;

    @Operation(summary = "Получить все товары", description = "Возвращает пагинированный список всех товаров")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @Parameter(description = "Номер страницы (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Поле для сортировки") @RequestParam(defaultValue = "name") String sort,
            @Parameter(description = "Направление сортировки") @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<Product> products = productService.getAllProducts(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("products", products.getContent());
        response.put("currentPage", products.getNumber());
        response.put("totalItems", products.getTotalElements());
        response.put("totalPages", products.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Получить товар по ID")
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @Parameter(description = "ID товара") @PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @Operation(summary = "Создать новый товар")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return ResponseEntity.ok(createdProduct);
    }

    @Operation(summary = "Обновить товар")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Product> updateProduct(
            @Parameter(description = "ID товара") @PathVariable Long id,
            @Valid @RequestBody Product product) {
        Product updatedProduct = productService.updateProduct(id, product);
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Удалить товар (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @Parameter(description = "ID товара") @PathVariable Long id) {
        productService.softDeleteProduct(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Product deleted successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Поиск товаров")
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @Parameter(description = "Поисковый запрос") @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.searchProducts(query, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("products", products.getContent());
        response.put("currentPage", products.getNumber());
        response.put("totalItems", products.getTotalElements());
        response.put("totalPages", products.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Фильтрация товаров")
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterProducts(
            @Parameter(description = "Название товара") @RequestParam(required = false) String name,
            @Parameter(description = "ID категории") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "ID бренда") @RequestParam(required = false) Long brandId,
            @Parameter(description = "Минимальная цена") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Максимальная цена") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Цвет") @RequestParam(required = false) String color,
            @Parameter(description = "Размер") @RequestParam(required = false) String sizeFilter,
            @Parameter(description = "Страна производства") @RequestParam(required = false) String country,
            @Parameter(description = "Целевая аудитория") @RequestParam(required = false) TargetAudience audience,
            @Parameter(description = "Тег товара") @RequestParam(required = false) ProductTag tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<Product> products = productService.filterProducts(name, categoryId, brandId,
                minPrice, maxPrice, color, sizeFilter, country, audience, tag, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("products", products.getContent());
        response.put("currentPage", products.getNumber());
        response.put("totalItems", products.getTotalElements());
        response.put("totalPages", products.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Получить новинки")
    @GetMapping("/new-arrivals")
    public ResponseEntity<List<Product>> getNewArrivals(
            @Parameter(description = "Лимит товаров") @RequestParam(defaultValue = "8") int limit) {
        List<Product> newArrivals = productService.getNewArrivals(limit);
        return ResponseEntity.ok(newArrivals);
    }

    @Operation(summary = "Получить товары со скидкой")
    @GetMapping("/sale")
    public ResponseEntity<List<Product>> getSaleProducts(
            @Parameter(description = "Лимит товаров") @RequestParam(defaultValue = "8") int limit) {
        List<Product> saleProducts = productService.getSaleProducts(limit);
        return ResponseEntity.ok(saleProducts);
    }

    @Operation(summary = "Получить популярные товары")
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Product> products = productService.getPopularProducts(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("products", products.getContent());
        response.put("currentPage", products.getNumber());
        response.put("totalItems", products.getTotalElements());
        response.put("totalPages", products.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Обновить количество товара на складе")
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Product> updateStock(
            @Parameter(description = "ID товара") @PathVariable Long id,
            @Parameter(description = "Новое количество") @RequestParam Integer quantity) {
        Product product = productService.getProductById(id);
        product.setStockQuantity(quantity);
        Product updatedProduct = productService.saveProduct(product);
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Добавить тег к товару")
    @PostMapping("/{id}/tags")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Product> addTag(
            @Parameter(description = "ID товара") @PathVariable Long id,
            @Parameter(description = "Тег") @RequestParam ProductTag tag) {
        Product product = productService.getProductById(id);
        product.addTag(tag);
        Product updatedProduct = productService.saveProduct(product);
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Удалить тег у товара")
    @DeleteMapping("/{id}/tags")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Product> removeTag(
            @Parameter(description = "ID товара") @PathVariable Long id,
            @Parameter(description = "Тег") @RequestParam ProductTag tag) {
        Product product = productService.getProductById(id);
        product.removeTag(tag);
        Product updatedProduct = productService.saveProduct(product);
        return ResponseEntity.ok(updatedProduct);
    }
}