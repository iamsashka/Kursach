package com.example.clothingstore.service;

import com.example.clothingstore.model.Category;
import com.example.clothingstore.repository.CategoryRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByDeletedFalse();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));
    }

    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = getCategoryById(id);
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        return categoryRepository.save(category);
    }

    public void softDeleteCategory(Long id) {
        Category category = getCategoryById(id);
        category.setDeleted(true);
        categoryRepository.save(category);
    }

    public void hardDeleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    public Page<Category> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public Page<Category> getActiveCategories(Pageable pageable) {
        return categoryRepository.findByDeletedFalse(pageable);
    }

    public Page<Category> getArchivedCategories(Pageable pageable) {
        return categoryRepository.findByDeletedTrue(pageable);
    }

    public Page<Category> searchCategories(String search, Pageable pageable) {
        return categoryRepository.searchCategories(search, pageable);
    }
    public List<Category> getActiveCategories() {
        return categoryRepository.findByDeletedFalse();
    }

    public List<Category> getArchivedCategories() {
        return categoryRepository.findByDeletedTrue();
    }

    public List<Category> searchCategories(String search) {
        return categoryRepository.searchCategories(search);
    }

    public long getTotalCategoriesCount() {
        return categoryRepository.count();
    }

    public long getActiveCategoriesCount() {
        return categoryRepository.countByDeletedFalse();
    }

    public long getArchivedCategoriesCount() {
        return categoryRepository.countByDeletedTrue();
    }

    public long getTotalProductsCount() {
        return 0L;
    }

    public void restoreCategory(Long id) {
        Category category = getCategoryById(id);
        category.setDeleted(false);
        categoryRepository.save(category);
    }

    public void exportToExcel(List<Category> categories, HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Categories");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Название");
            headerRow.createCell(2).setCellValue("Описание");
            headerRow.createCell(3).setCellValue("Статус");

            int rowNum = 1;
            for (Category category : categories) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(category.getId());
                row.createCell(1).setCellValue(category.getName());
                row.createCell(2).setCellValue(category.getDescription() != null ? category.getDescription() : "");
                row.createCell(3).setCellValue(category.isDeleted() ? "Архив" : "Активна");
            }

            workbook.write(response.getOutputStream());
        }
    }

    public void exportToPdf(List<Category> categories, HttpServletResponse response) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("Категории\n\n");

        for (Category category : categories) {
            content.append(String.format("ID: %d | Название: %s | Статус: %s\n",
                    category.getId(),
                    category.getName(),
                    category.isDeleted() ? "Архив" : "Активна"));
        }

        response.getWriter().write(content.toString());
    }
}