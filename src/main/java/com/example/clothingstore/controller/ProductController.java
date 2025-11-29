package com.example.clothingstore.controller;
import com.example.clothingstore.service.*;
import com.example.clothingstore.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final UserService userService;
    private final FavoriteService favoriteService;
    private final MetricsService metricsService;

    public ProductController(ProductService productService,
                             CategoryService categoryService,
                             BrandService brandService,
                             UserService userService,
                             FavoriteService favoriteService,
                             MetricsService metricsService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.userService = userService;
        this.favoriteService = favoriteService;
        this.metricsService = metricsService;
    }
    @GetMapping("/{id}")
    public String getProductDetails(@PathVariable Long id,
                                    @RequestParam(required = false) Integer imageIndex,
                                    @RequestParam(required = false) Integer quantity,
                                    @RequestParam(required = false) Boolean favorite,
                                    HttpServletRequest request,
                                    Model model) {
        try {
            Product product = productService.getProductByIdIgnoreDeleted(id);

        product.incrementViews();
        productService.save(product);
        List<Product> related = productService.getRelatedProducts(product);

        // Собираем дополнительные изображения
        List<String> additionalImages = new ArrayList<>();
        List<ProductImage> productImages = product.getImages();
        if (productImages != null && !productImages.isEmpty()) {
            for (ProductImage image : productImages) {
                if (image.getImageUrl() != null && !image.getImageUrl().trim().isEmpty()) {
                    additionalImages.add(image.getImageUrl());
                }
            }
        }

        // Определяем текущее изображение
        String currentImage = product.getMainImage();
        int currentIndex = -1;

        if (imageIndex != null && imageIndex >= 0 && imageIndex < additionalImages.size()) {
            currentImage = additionalImages.get(imageIndex);
            currentIndex = imageIndex;
        }

        // Проверяем избранное
        boolean isInFavorites = false;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                String email = authentication.getName();
                User user = userService.findByEmail(email).orElse(null);
                if (user != null) {
                    isInFavorites = favoriteService.isProductInFavorites(user, product.getId());
                }
            } catch (Exception e) {
                // Игнорируем ошибки при проверке избранного
            }
        }

        // Если передан параметр favorite, используем его
        if (favorite != null) {
            isInFavorites = favorite;
        }

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", related);
        model.addAttribute("additionalImages", additionalImages);
        model.addAttribute("currentImage", currentImage);
        model.addAttribute("currentImageIndex", currentIndex);
        model.addAttribute("quantity", quantity != null ? quantity : 1);
        model.addAttribute("isInFavorites", isInFavorites);

        return "products/detail";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Товар с ID " + id + " не найден");
            return "error";
        }
    }
    @GetMapping
    public String getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Model model) {

        // Обработка пустых строк как null
        String processedSearch = (search != null && !search.trim().isEmpty()) ? search : null;
        Long processedCategoryId = (categoryId != null && categoryId > 0) ? categoryId : null;
        Long processedBrandId = (brandId != null && brandId > 0) ? brandId : null;
        Double processedMinPrice = (minPrice != null && minPrice > 0) ? minPrice : null;
        Double processedMaxPrice = (maxPrice != null && maxPrice > 0) ? maxPrice : null;

        Sort sort = Sort.by(sortBy);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage;

        if (processedSearch != null || processedCategoryId != null || processedBrandId != null ||
                processedMinPrice != null || processedMaxPrice != null) {
            productPage = productService.filterProducts(processedSearch, processedCategoryId, processedBrandId,
                    processedMinPrice, processedMaxPrice, pageable);
        } else {
            productPage = productService.getAllProducts(pageable);
        }

        model.addAttribute("products", productPage);
        model.addAttribute("categories", categoryService.getAllActiveCategories());
        model.addAttribute("brands", brandService.getAllActiveBrands());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("search", processedSearch);
        model.addAttribute("categoryId", processedCategoryId);
        model.addAttribute("brandId", processedBrandId);
        model.addAttribute("minPrice", processedMinPrice);
        model.addAttribute("maxPrice", processedMaxPrice);
        model.addAttribute("size", size);

        return "products/list";
    }
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        try {
            System.out.println("=== DEBUG: Starting showCreateForm ===");

            model.addAttribute("product", new Product());
            System.out.println("=== DEBUG: Product created ===");

            try {
                var categories = categoryService.getAllActiveCategories();
                System.out.println("=== DEBUG: Categories retrieved, count: " + categories.size() + " ===");
                model.addAttribute("categories", categories);
            } catch (Exception e) {
                System.out.println("=== ERROR getting categories: " + e.getMessage() + " ===");
                e.printStackTrace();
                return "error";
            }

            try {
                var brands = brandService.getAllActiveBrands();
                System.out.println("=== DEBUG: Brands retrieved, count: " + brands.size() + " ===");
                model.addAttribute("brands", brands);
            } catch (Exception e) {
                System.out.println("=== ERROR getting brands: " + e.getMessage() + " ===");
                e.printStackTrace();
                return "error";
            }

            System.out.println("=== DEBUG: Returning products/form ===");
            return "products/form";

        } catch (Exception e) {
            System.out.println("=== FINAL ERROR in showCreateForm: " + e.getMessage() + " ===");
            e.printStackTrace();
            throw e;
        }
    }
    @GetMapping("/archive")
    public String showArchivedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        System.out.println("=== ARCHIVE DEBUG START ===");

        // Убираем сортировку по updatedAt, используем сортировку по ID или имени
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // Используем productService вместо productRepository
        Page<Product> archivedProducts = productService.getArchivedProducts(pageable);

        // Диагностика
        System.out.println("Total archived products: " + archivedProducts.getTotalElements());
        System.out.println("Current page: " + page + ", size: " + size);
        System.out.println("Total pages: " + archivedProducts.getTotalPages());

        // Выведем информацию о каждом товаре в архиве
        archivedProducts.getContent().forEach(product -> {
            System.out.println("Archived product - ID: " + product.getId() +
                    ", Name: " + product.getName() +
                    ", Deleted: " + product.isDeleted());
        });

        // Получаем статистику через сервис
        long totalProductsCount = productService.getTotalProductsCount();
        long activeProductsCount = productService.getActiveProductsCount();
        long archivedProductsCount = productService.getArchivedProductsCount();

        System.out.println("Statistics - Total: " + totalProductsCount +
                ", Active: " + activeProductsCount +
                ", Archived: " + archivedProductsCount);

        System.out.println("=== ARCHIVE DEBUG END ===");

        model.addAttribute("products", archivedProducts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", archivedProducts.getTotalPages());
        model.addAttribute("totalProductsCount", totalProductsCount);
        model.addAttribute("activeProductsCount", activeProductsCount);
        model.addAttribute("archivedProductsCount", archivedProductsCount);

        return "products/archive";
    }
    @PostMapping("/create")
    public String createProduct(@RequestParam String name,
                                @RequestParam String description,
                                @RequestParam BigDecimal price,
                                @RequestParam Integer stockQuantity,
                                @RequestParam(required = false) String sizes,
                                @RequestParam Long categoryId,
                                @RequestParam Long brandId,
                                HttpServletRequest request,
                                Model model) {

        try {
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setStockQuantity(stockQuantity);
            product.setSizes(sizes);

            Category category = categoryService.getCategoryById(categoryId);
            Brand brand = brandService.getBrandById(brandId);

            product.setCategory(category);
            product.setBrand(brand);
            productService.createProduct(product, request);

            productService.saveProduct(product);
            return "redirect:/products";

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка создания товара: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAllActiveCategories());
            model.addAttribute("brands", brandService.getAllActiveBrands());
            return "products/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllActiveCategories());
        model.addAttribute("brands", brandService.getAllActiveBrands());
        return "products/form";
    }

    @PostMapping("/update/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("product") Product product,
                                HttpServletRequest request,
                                BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllActiveCategories());
            model.addAttribute("brands", brandService.getAllActiveBrands());
            return "products/form";
        }
        productService.updateProduct(id, product);
        productService.updateProduct(id, product, request);
        return "redirect:/products";
    }
    @GetMapping("/soft-delete/{id}")
    public String softDeleteProduct(@PathVariable Long id, HttpServletRequest request) {
        System.out.println("=== SOFT DELETE DEBUG START ===");

        try {
            Product product = productService.getProductById(id);
            System.out.println("BEFORE - ID: " + product.getId() + ", Name: " + product.getName() + ", Deleted: " + product.isDeleted());

            productService.softDeleteProduct(id);

            // Проверим через сервис
            Product afterDelete = productService.getProductByIdIgnoreDeleted(id);
            System.out.println("AFTER - ID: " + afterDelete.getId() + ", Name: " + afterDelete.getName() + ", Deleted: " + afterDelete.isDeleted());

        } catch (Exception e) {
            System.out.println("ERROR in softDelete: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== SOFT DELETE DEBUG END ===");
        productService.softDeleteProduct(id, request);
        return "redirect:/products";
    }
    @GetMapping("/hard-delete/{id}")
    public String hardDeleteProduct(@PathVariable Long id) {
        productService.hardDeleteProduct(id);
        return "redirect:/products";
    }


    @GetMapping("/restore/{id}")
    public String restoreProduct(@PathVariable Long id, HttpServletRequest request) {
        productService.restoreProduct(id);
        productService.restoreProduct(id, request);
        return "redirect:/products/archive";
    }
    @GetMapping("/debug/archived")
    @ResponseBody
    public String debugArchivedProducts() {
        StringBuilder result = new StringBuilder();
        result.append("=== ARCHIVED PRODUCTS DEBUG ===\n");

        // Получим все удаленные товары без пагинации
        List<Product> allArchived = productService.getAllArchivedProducts();
        result.append("Total archived products: ").append(allArchived.size()).append("\n\n");

        for (Product product : allArchived) {
            result.append(String.format("ID: %d, Name: %s, Deleted: %s\n",
                    product.getId(),
                    product.getName(),
                    product.isDeleted()));
        }

        return result.toString();
    }

    @GetMapping("/export/excel")
    public void exportToExcel(HttpServletResponse response,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(required = false) Long brandId,
                              @RequestParam(required = false) Double minPrice,
                              @RequestParam(required = false) Double maxPrice) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=products_" + java.time.LocalDate.now() + ".xlsx";
        response.setHeader(headerKey, headerValue);

        List<Product> products;
        if (search != null && !search.isBlank()) {
            products = productService.searchProductsForExport(search);
        } else if (categoryId != null || brandId != null || minPrice != null || maxPrice != null) {
            products = productService.getFilteredProductsForExport(categoryId, brandId, minPrice, maxPrice);
        } else {
            products = productService.getAllProductsForExport();
        }

        productService.exportToExcel(products, response);
    }

    @GetMapping("/export/pdf")
    public void exportToPdf(HttpServletResponse response,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) Long brandId,
                            @RequestParam(required = false) Double minPrice,
                            @RequestParam(required = false) Double maxPrice) throws IOException {

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=products_" + java.time.LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);

        List<Product> products;
        if (search != null && !search.isBlank()) {
            products = productService.searchProductsForExport(search);
        } else if (categoryId != null || brandId != null || minPrice != null || maxPrice != null) {
            products = productService.getFilteredProductsForExport(categoryId, brandId, minPrice, maxPrice);
        } else {
            products = productService.getAllProductsForExport();
        }

        productService.exportToPdf(products, response);
    }
}