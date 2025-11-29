package com.example.clothingstore.controller;

import com.example.clothingstore.model.Category;
import com.example.clothingstore.service.CategoryService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    @GetMapping
    public String getAllCategories(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        Page<Category> categoryPage;
        if (search != null && !search.isBlank()) {
            categoryPage = categoryService.searchCategories(search, pageable);
        } else if (status != null && !status.isEmpty()) {
            if ("active".equals(status)) {
                categoryPage = categoryService.getActiveCategories(pageable);
            } else if ("archived".equals(status)) {
                categoryPage = categoryService.getArchivedCategories(pageable);
            } else {
                categoryPage = categoryService.getAllCategories(pageable);
            }
        } else {
            categoryPage = categoryService.getAllCategories(pageable);
        }

        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("category", new Category());
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalCategoriesCount", categoryService.getTotalCategoriesCount());
        model.addAttribute("activeCategoriesCount", categoryService.getActiveCategoriesCount());
        model.addAttribute("archivedCategoriesCount", categoryService.getArchivedCategoriesCount());
        model.addAttribute("totalProductsCount", categoryService.getTotalProductsCount());

        return "categories/list";
    }
    @GetMapping("/archive")
    public String getArchivedCategories(Model model,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Category> archivedCategories = categoryService.getArchivedCategories(pageable);

        model.addAttribute("categories", archivedCategories.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", archivedCategories.getTotalPages());
        model.addAttribute("totalCategoriesCount", categoryService.getTotalCategoriesCount());
        model.addAttribute("activeCategoriesCount", categoryService.getActiveCategoriesCount());
        model.addAttribute("archivedCategoriesCount", categoryService.getArchivedCategoriesCount());

        return "categories/archive";
    }

    @GetMapping("/restore/{id}")
    public String restoreCategory(@PathVariable Long id) {
        categoryService.restoreCategory(id);
        return "redirect:/categories";
    }


    @GetMapping("/export/excel")
    public void exportToExcel(HttpServletResponse response,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) String status) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=categories_" + LocalDate.now() + ".xlsx";
        response.setHeader(headerKey, headerValue);

        List<Category> categories;
        if (search != null && !search.isBlank()) {
            categories = categoryService.searchCategories(search);
        } else if (status != null && !status.isEmpty()) {
            if ("active".equals(status)) {
                categories = categoryService.getActiveCategories();
            } else if ("archived".equals(status)) {
                categories = categoryService.getArchivedCategories();
            } else {
                categories = categoryService.getAllCategories();
            }
        } else {
            categories = categoryService.getAllCategories();
        }

        categoryService.exportToExcel(categories, response);
    }

    @GetMapping("/export/pdf")
    public void exportToPdf(HttpServletResponse response,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String status) throws IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=categories_" + LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);

        List<Category> categories;
        if (search != null && !search.isBlank()) {
            categories = categoryService.searchCategories(search);
        } else if (status != null && !status.isEmpty()) {
            if ("active".equals(status)) {
                categories = categoryService.getActiveCategories();
            } else if ("archived".equals(status)) {
                categories = categoryService.getArchivedCategories();
            } else {
                categories = categoryService.getAllCategories();
            }
        } else {
            categories = categoryService.getAllCategories();
        }

        categoryService.exportToPdf(categories, response);
    }
    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("category") Category category,
                                 BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "categories/list";
        }
        categoryService.createCategory(category);
        return "redirect:/categories";
    }

    @GetMapping("/edit/{id}")
    public String editCategory(@PathVariable Long id, Model model) {
        try {
            System.out.println("=== DEBUG: Starting editCategory for id: " + id + " ===");

            Category category = categoryService.getCategoryById(id);
            System.out.println("=== DEBUG: Category found: " + category.getName() + " ===");

            model.addAttribute("category", category);
            System.out.println("=== DEBUG: Returning categories/form ===");

            return "categories/form";
        } catch (Exception e) {
            System.out.println("=== ERROR in editCategory: " + e.getMessage() + " ===");
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("category") Category category,
                                 BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "categories/form";
        }
        categoryService.updateCategory(id, category);
        return "redirect:/categories";
    }

    @GetMapping("/soft-delete/{id}")
    public String softDeleteCategory(@PathVariable Long id) {
        categoryService.softDeleteCategory(id);
        return "redirect:/categories";
    }

    @GetMapping("/hard-delete/{id}")
    public String hardDeleteCategory(@PathVariable Long id) {
        categoryService.hardDeleteCategory(id);
        return "redirect:/categories";
    }
}