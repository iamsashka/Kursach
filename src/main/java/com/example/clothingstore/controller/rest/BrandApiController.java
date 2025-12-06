package com.example.clothingstore.controller.rest;

import com.example.clothingstore.model.Brand;
import com.example.clothingstore.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@Tag(name = "Brands API", description = "API для работы с брендами")
public class BrandApiController {

    private final BrandService brandService;

    public BrandApiController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    @Operation(summary = "Получить все активные бренды", description = "Публичный доступ")
    public ResponseEntity<List<Brand>> getAllActiveBrands() {
        List<Brand> brands = brandService.getAllActiveBrands();
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить бренд по ID")
    public ResponseEntity<Brand> getBrandById(
            @Parameter(description = "ID бренда") @PathVariable Long id) {
        Brand brand = brandService.getBrandById(id);
        return ResponseEntity.ok(brand);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Получить все бренды с пагинацией", description = "Только для администраторов и менеджеров")
    public ResponseEntity<Page<Brand>> getAllBrands(
            @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Поле для сортировки") @RequestParam(defaultValue = "name") String sort,
            @Parameter(description = "Направление сортировки") @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<Brand> brands = brandService.getAllBrands(pageable);
        return ResponseEntity.ok(brands);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Создать новый бренд")
    public ResponseEntity<Brand> createBrand(@Valid @RequestBody Brand brand) {
        Brand createdBrand = brandService.createBrand(brand);
        return ResponseEntity.ok(createdBrand);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Обновить бренд")
    public ResponseEntity<Brand> updateBrand(
            @Parameter(description = "ID бренда") @PathVariable Long id,
            @Valid @RequestBody Brand brand) {
        Brand updatedBrand = brandService.updateBrand(id, brand);
        return ResponseEntity.ok(updatedBrand);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Удалить бренд (soft delete)")
    public ResponseEntity<Void> deleteBrand(
            @Parameter(description = "ID бренда") @PathVariable Long id) {
        brandService.softDeleteBrand(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск брендов")
    public ResponseEntity<List<Brand>> searchBrands(
            @Parameter(description = "Поисковый запрос") @RequestParam String query) {
        List<Brand> brands = brandService.searchBrands(query);
        return ResponseEntity.ok(brands);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Восстановить удаленный бренд")
    public ResponseEntity<Brand> restoreBrand(
            @Parameter(description = "ID бренда") @PathVariable Long id) {
        Brand restoredBrand = brandService.restoreBrand(id);
        return ResponseEntity.ok(restoredBrand);
    }
}