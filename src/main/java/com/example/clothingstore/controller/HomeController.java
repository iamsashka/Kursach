package com.example.clothingstore.controller;

import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.TargetAudience;
import com.example.clothingstore.service.ProductService;
import com.example.clothingstore.service.ThemeService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final ProductService productService;
    private final ThemeService themeService;

    public HomeController(ProductService productService, ThemeService themeService) {
        this.productService = productService;
        this.themeService = themeService;
    }

    @GetMapping({"/", "/index"})
    public String home(Authentication authentication,
                       @RequestParam(value = "registered", required = false) String registered,
                       HttpServletRequest request, // ✅ Добавляем request
                       Model model) {

        // ✅ ДОБАВЛЯЕМ ТЕМУ И ПАРАМЕТРЫ СТРАНИЦЫ
        String currentTheme = themeService.getCurrentTheme(request);
        model.addAttribute("currentTheme", currentTheme != null ? currentTheme : "light");
        model.addAttribute("currentPage", "/index");

        if (registered != null) {
            model.addAttribute("success", "Регистрация прошла успешно! Добро пожаловать в наш магазин!");
        }

        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ROLE_ADMIN"));

            boolean isManager = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ROLE_MANAGER"));

            if (isAdmin || isManager) {
                // ✅ Добавляем атрибуты для админской панели
                model.addAttribute("isAdmin", true);
                model.addAttribute("userEmail", authentication.getName());
                model.addAttribute("pageTitle", "Админ панель TARENO");
                return "index"; // Возвращаем index.html для админов
            } else {
                return "redirect:/home";
            }
        }

        return "redirect:/home";
    }
    @GetMapping("/home")
    public String customerHome(Authentication authentication,
                               @RequestParam(value = "registered", required = false) String registered,
                               @RequestParam(value = "subscribed", required = false) String subscribed,
                               HttpServletRequest request,
                               Model model) {

        logger.info("=== Loading home page for user: {} ===",
                authentication != null ? authentication.getName() : "anonymous");

        // Добавляем текущую тему в модель с значением по умолчанию
        String currentTheme = themeService.getCurrentTheme(request);
        model.addAttribute("currentTheme", currentTheme != null ? currentTheme : "light");
        model.addAttribute("currentPage", "/home");

        try {
            if (registered != null) {
                model.addAttribute("success", "Регистрация прошла успешно! Добро пожаловать в наш магазин!");
            }
            if (subscribed != null) {
                model.addAttribute("success", "Вы успешно подписались на рассылку!");
            }

            // ФИКС: Добавляем обязательные атрибуты для безопасности
            model.addAttribute("title", "TARENO - Премиальная одежда");
            model.addAttribute("success", model.containsAttribute("success") ? model.getAttribute("success") : null);
            model.addAttribute("error", null);

            // Безопасная загрузка данных
            List<SafeProduct> safeNewArrivals = getSafeProducts("newArrivals", 8);
            List<SafeProduct> safeBestSellers = getSafeProducts("bestSellers", 8);
            List<SafeProduct> safeSaleProducts = getSafeProducts("saleProducts", 8);

            logger.info("Loaded products - New: {}, Best: {}, Sale: {}",
                    safeNewArrivals.size(), safeBestSellers.size(), safeSaleProducts.size());

            model.addAttribute("newArrivals", safeNewArrivals);
            model.addAttribute("bestSellers", safeBestSellers);
            model.addAttribute("saleProducts", safeSaleProducts);
            model.addAttribute("targetAudiences", TargetAudience.values());

        } catch (Exception e) {
            logger.error("CRITICAL ERROR loading home page: ", e);

            model.addAttribute("title", "TARENO - Премиальная одежда");
            model.addAttribute("newArrivals", new ArrayList<SafeProduct>());
            model.addAttribute("bestSellers", new ArrayList<SafeProduct>());
            model.addAttribute("saleProducts", new ArrayList<SafeProduct>());
            model.addAttribute("targetAudiences", TargetAudience.values());
            model.addAttribute("error", "Временные технические неполадки");
        }

        return "home";
    }

    private List<SafeProduct> getSafeProducts(String type, int limit) {
        List<SafeProduct> safeProducts = new ArrayList<>();

        try {
            List<Product> products;

            switch (type) {
                case "newArrivals":
                    products = productService.getNewArrivals(limit);
                    break;
                case "bestSellers":
                    products = productService.getBestSellers(limit);
                    break;
                case "saleProducts":
                    products = productService.getSaleProducts(limit);
                    break;
                default:
                    products = new ArrayList<>();
            }

            for (Product product : products) {
                try {
                    SafeProduct safeProduct = new SafeProduct(product);
                    safeProducts.add(safeProduct);
                } catch (Exception e) {
                    logger.warn("Skipping problematic product {}: {}", product.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error loading {}: {}", type, e.getMessage());
        }

        return safeProducts;
    }

    // Вспомогательный класс для безопасного отображения продуктов
    public static class SafeProduct {
        private Long id;
        private String name;
        private String brandName;
        private String price;
        private String imageUrl;
        private boolean hasImage;
        private boolean onSale;
        private String originalPrice;
        private String discountPercent;

        public SafeProduct(Product product) {
            this.id = product.getId();
            this.name = safeString(product.getName(), "Товар");
            this.brandName = safeString(product.getBrand() != null ? product.getBrand().getName() : null, "Бренд");

            // Обработка цен и скидок
            if (product.getPrice() != null) {
                this.price = "₽" + String.format("%,.0f", product.getPrice().doubleValue());
            } else {
                this.price = "₽0";
            }

            this.onSale = product.isOnSale();
            if (this.onSale && product.getOriginalPrice() != null) {
                this.originalPrice = "₽" + String.format("%,.0f", product.getOriginalPrice().doubleValue());
                this.discountPercent = "-" + product.getDiscountPercent().intValue() + "%";
            } else {
                this.originalPrice = "";
                this.discountPercent = "";
            }

            // Получаем реальное изображение продукта
            String mainImage = product.getMainImage();
            this.hasImage = mainImage != null && !mainImage.trim().isEmpty();
            this.imageUrl = this.hasImage ? mainImage : "";
        }

        private String safeString(String value, String defaultValue) {
            return value != null && !value.trim().isEmpty() ? value : defaultValue;
        }

        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getBrandName() { return brandName; }
        public String getPrice() { return price; }
        public String getImageUrl() { return imageUrl; }
        public boolean isHasImage() { return hasImage; }
        public boolean isOnSale() { return onSale; }
        public String getOriginalPrice() { return originalPrice; }
        public String getDiscountPercent() { return discountPercent; }
    }

    @GetMapping("/subscribe")
    public String subscribeToNewsletter(@RequestParam String email,
                                        HttpServletRequest request,
                                        Model model) {
        logger.info("New subscriber: {}", email);
        // Добавляем тему для redirect страницы
        model.addAttribute("currentTheme", themeService.getCurrentTheme(request));
        return "redirect:/home?subscribed=true";
    }

    @GetMapping("/home/men")
    public String redirectToMen() {
        return "redirect:/catalog/men";
    }

    @GetMapping("/home/women")
    public String redirectToWomen() {
        return "redirect:/catalog/women";
    }

    @GetMapping("/home/teens")
    public String redirectToTeens() {
        return "redirect:/catalog/teens";
    }

    @GetMapping("/home/kids")
    public String redirectToKids() {
        return "redirect:/catalog/kids";
    }

    @GetMapping("/home/new-arrivals")
    public String redirectToNewArrivals() {
        return "redirect:/catalog/new-arrivals";
    }

    @GetMapping("/home/sale")
    public String redirectToSale() {
        return "redirect:/catalog/sale";
    }

    @GetMapping("/home/bestsellers")
    public String redirectToBestsellers() {
        return "redirect:/catalog/bestsellers";
    }
}