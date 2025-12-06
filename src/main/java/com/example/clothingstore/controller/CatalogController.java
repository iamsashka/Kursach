package com.example.clothingstore.controller;

import com.example.clothingstore.model.*;
import com.example.clothingstore.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/catalog")
public class CatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public CatalogController(ProductService productService,
                             CategoryService categoryService,
                             BrandService brandService,
                             UserService userService,
                             ObjectMapper objectMapper,
                             MetricsService metricsService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    @GetMapping
    public String catalog(Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "12") int size,
                          @RequestParam(defaultValue = "name") String sortBy,
                          @RequestParam(defaultValue = "asc") String sortDir,
                          @RequestParam(required = false) Long categoryId,
                          @RequestParam(required = false) Long brandId,
                          @RequestParam(required = false) BigDecimal minPrice,
                          @RequestParam(required = false) BigDecimal maxPrice,
                          @RequestParam(required = false) String color,
                          @RequestParam(required = false) String sizeFilter,
                          @RequestParam(required = false) String country,
                          @RequestParam(required = false) TargetAudience audience,
                          @RequestParam(required = false) ProductTag tag,
                          @RequestParam(required = false) String search,
                          Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            metricsService.userLoggedIn();

            Map<String, Object> currentFilters = new HashMap<>();
            currentFilters.put("search", search);
            currentFilters.put("categoryId", categoryId);
            currentFilters.put("brandId", brandId);
            currentFilters.put("minPrice", minPrice);
            currentFilters.put("maxPrice", maxPrice);
            currentFilters.put("color", color);
            currentFilters.put("sizeFilter", sizeFilter);
            currentFilters.put("country", country);
            currentFilters.put("audience", audience);
            currentFilters.put("tag", tag);
            currentFilters.put("savedAt", System.currentTimeMillis());

            saveCurrentFilters(authentication, currentFilters);
        }
        int pageSize = determinePageSize(size, authentication);
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Page<Product> productPage;

        if (hasFilters(categoryId, brandId, minPrice, maxPrice, color, sizeFilter, country, audience, tag, search)) {
            productPage = productService.filterProducts(
                    search, categoryId, brandId, minPrice, maxPrice,
                    color, sizeFilter, country, audience, tag, pageable
            );
        } else {
            productPage = productService.getAllProducts(pageable);
        }

        List<Category> categories = categoryService.getAllActiveCategories();
        List<Brand> brands = brandService.getAllActiveBrands();
        List<String> availableColors = productService.getAvailableColors();
        List<String> availableSizes = productService.getAvailableSizes();
        List<String> availableCountries = productService.getAvailableCountries();

        Category selectedCategory = null;
        Brand selectedBrand = null;

        if (categoryId != null) {
            selectedCategory = categories.stream()
                    .filter(c -> c.getId().equals(categoryId))
                    .findFirst()
                    .orElse(null);
        }

        if (brandId != null) {
            selectedBrand = brands.stream()
                    .filter(b -> b.getId().equals(brandId))
                    .findFirst()
                    .orElse(null);
        }

        model.addAttribute("title", "Каталог товаров");
        model.addAttribute("products", productPage);

        model.addAttribute("categories", categories);
        model.addAttribute("brands", brands);
        model.addAttribute("availableColors", availableColors);
        model.addAttribute("availableSizes", availableSizes);
        model.addAttribute("availableCountries", availableCountries);
        model.addAttribute("targetAudiences", TargetAudience.values());
        model.addAttribute("productTags", ProductTag.values());

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        model.addAttribute("categoryId", categoryId);
        model.addAttribute("brandId", brandId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("color", color);
        model.addAttribute("sizeFilter", sizeFilter);
        model.addAttribute("country", country);
        model.addAttribute("audience", audience);
        model.addAttribute("tag", tag);
        model.addAttribute("search", search);

        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("selectedBrand", selectedBrand);

        return "catalog/index";
    }

    @GetMapping("/new-arrivals")
    public String newArrivals(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              Authentication authentication) {
        int pageSize = determinePageSize(size, authentication);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Product> productPage = productService.getProductsByTag(ProductTag.NEW_ARRIVAL, pageable);

        setupCatalogModel(model, productPage, page);
        model.addAttribute("title", "Новинки");
        model.addAttribute("tag", ProductTag.NEW_ARRIVAL);

        return "catalog/index";
    }

    @GetMapping("/sale")
    public String saleProducts(Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size,
                               Authentication authentication) {
        int pageSize = determinePageSize(size, authentication);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Product> productPage = productService.getProductsByTag(ProductTag.SALE, pageable);

        setupCatalogModel(model, productPage, page);
        model.addAttribute("title", "Распродажа");
        model.addAttribute("tag", ProductTag.SALE);

        return "catalog/index";
    }

    @GetMapping("/bestsellers")
    public String bestsellers(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              Authentication authentication) {
        int pageSize = determinePageSize(size, authentication);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Product> productPage = productService.getProductsByTag(ProductTag.BESTSELLER, pageable);

        setupCatalogModel(model, productPage, page);
        model.addAttribute("title", "Хиты продаж");
        model.addAttribute("tag", ProductTag.BESTSELLER);

        return "catalog/index";
    }

    @GetMapping("/men")
    public String menProducts(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size) {
        return productsByAudience(model, TargetAudience.MEN, "Мужская одежда", page, size);
    }

    @GetMapping("/women")
    public String womenProducts(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size) {
        return productsByAudience(model, TargetAudience.WOMEN, "Женская одежда", page, size);
    }

    @GetMapping("/teens")
    public String teensProducts(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size) {
        return productsByAudience(model, TargetAudience.TEENS, "Одежда для подростков", page, size);
    }

    @GetMapping("/kids")
    public String kidsProducts(Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size) {
        return productsByAudience(model, TargetAudience.KIDS, "Детская одежда", page, size);
    }

    @GetMapping("/search")
    public String searchProducts(Model model,
                                 @RequestParam String q,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productService.searchProducts(q, pageable);

        setupCatalogModel(model, productPage, page);
        model.addAttribute("title", "Результаты поиска: " + q);
        model.addAttribute("search", q);

        return "catalog/index";
    }

    private boolean hasFilters(Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice,
                               String color, String size, String country, TargetAudience audience,
                               ProductTag tag, String search) {
        return categoryId != null || brandId != null || minPrice != null || maxPrice != null ||
                color != null || size != null || country != null || audience != null ||
                tag != null || (search != null && !search.trim().isEmpty());
    }

    private void setupCatalogModel(Model model, Page<Product> productPage, int page) {
        List<Category> categories = categoryService.getAllActiveCategories();
        List<Brand> brands = brandService.getAllActiveBrands();
        List<String> availableColors = productService.getAvailableColors();
        List<String> availableSizes = productService.getAvailableSizes();
        List<String> availableCountries = productService.getAvailableCountries();

        model.addAttribute("products", productPage);
        model.addAttribute("categories", categories);
        model.addAttribute("brands", brands);
        model.addAttribute("availableColors", availableColors);
        model.addAttribute("availableSizes", availableSizes);
        model.addAttribute("availableCountries", availableCountries);
        model.addAttribute("targetAudiences", TargetAudience.values());
        model.addAttribute("productTags", ProductTag.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        model.addAttribute("selectedCategory", null);
        model.addAttribute("selectedBrand", null);
    }

    private String productsByAudience(Model model, TargetAudience audience, String title,
                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productService.getProductsByAudience(audience, pageable);

        setupCatalogModel(model, productPage, page);
        model.addAttribute("title", title);
        model.addAttribute("audience", audience);

        return "catalog/index";
    }
    private void saveCurrentFilters(Authentication authentication, Map<String, Object> currentFilters) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Optional<User> user = userService.findByEmail(authentication.getName());
                if (user.isPresent()) {
                    boolean hasActiveFilters = currentFilters.entrySet().stream()
                            .anyMatch(entry -> entry.getValue() != null &&
                                    !entry.getKey().equals("savedAt"));

                    if (hasActiveFilters) {
                        String savedFilters = objectMapper.writeValueAsString(currentFilters);
                        user.get().setSavedFilters(savedFilters);
                        userService.save(user.get());
                    }
                }
            } catch (Exception e) {
                System.out.println("Ошибка сохранения фильтров: " + e.getMessage());
            }
        }
    }
    private int determinePageSize(int sizeFromUrl, Authentication authentication) {
        System.out.println("=== DEBUG: determinePageSize ===");
        System.out.println("sizeFromUrl: " + sizeFromUrl);

        if (sizeFromUrl != 12) {
            System.out.println("Using size from URL: " + sizeFromUrl);
            return sizeFromUrl;
        }

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Optional<User> user = userService.findByEmail(authentication.getName());
                if (user.isPresent() && user.get().getPageSize() != null) {
                    System.out.println("Using user setting: " + user.get().getPageSize());
                    return user.get().getPageSize();
                } else {
                    System.out.println("User setting not found");
                }
            } catch (Exception e) {
                System.out.println("Error getting user settings: " + e.getMessage());
            }
        } else {
            System.out.println("User not authenticated");
        }

        System.out.println("Using default: " + sizeFromUrl);
        return sizeFromUrl;
    }
}