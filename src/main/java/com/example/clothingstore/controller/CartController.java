package com.example.clothingstore.controller;

import com.example.clothingstore.model.CartItem;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.User;
import com.example.clothingstore.repository.CartItemRepository;
import com.example.clothingstore.repository.ProductRepository;
import com.example.clothingstore.repository.UserRepository;
import com.example.clothingstore.service.MetricsService;
import com.example.clothingstore.service.ThemeService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private final ThemeService themeService;
    private final MetricsService metricsService;

    public CartController(ThemeService themeService, MetricsService metricsService) {
        this.themeService = themeService;
        this.metricsService = metricsService;
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam int quantity,
                            @RequestParam(required = false) String size,
                            @RequestParam(required = false) String color,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {

        try {
            if (userDetails == null) {
                redirectAttributes.addFlashAttribute("error", "Для добавления в корзину необходимо авторизоваться");
                return "redirect:/login";
            }

            logger.info("Adding product {} to cart for user {}", productId, userDetails.getUsername());

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Товар не найден");
                return "redirect:/products/" + productId;
            }

            Product product = productOpt.get();

            if (product.getStockQuantity() < quantity) {
                redirectAttributes.addFlashAttribute("error",
                        "Недостаточно товара в наличии. Доступно: " + product.getStockQuantity() + " шт.");
                return "redirect:/products/" + productId;
            }

            String finalSize = (size != null && !size.trim().isEmpty()) ? size : "M";
            String finalColor = (color != null && !color.trim().isEmpty()) ? color : "Черный";

            Optional<CartItem> existingCartItem = cartItemRepository.findByUserAndProductAndSizeAndColor(
                    user, product, finalSize, finalColor
            );

            if (existingCartItem.isPresent()) {
                CartItem cartItem = existingCartItem.get();
                int newQuantity = cartItem.getQuantity() + quantity;

                if (newQuantity > product.getStockQuantity()) {
                    redirectAttributes.addFlashAttribute("error",
                            "Недостаточно товара в наличии. Доступно: " + product.getStockQuantity() + " шт.");
                    return "redirect:/products/" + productId;
                }

                cartItem.setQuantity(newQuantity);
                cartItemRepository.save(cartItem);
                redirectAttributes.addFlashAttribute("success", "Количество товара обновлено в корзине");
            } else {
                CartItem cartItem = new CartItem();
                cartItem.setUser(user);
                cartItem.setProduct(product);
                cartItem.setQuantity(quantity);
                cartItem.setSize(finalSize);
                cartItem.setColor(finalColor);
                cartItemRepository.save(cartItem);
                redirectAttributes.addFlashAttribute("success", "Товар добавлен в корзину");
            }

            return "redirect:/products/" + productId;

        } catch (Exception e) {
            logger.error("Error adding product to cart: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при добавлении товара в корзину");
            return "redirect:/products/" + productId;
        }
    }

    @GetMapping("/cart")
    public String cartPage(@AuthenticationPrincipal UserDetails userDetails,
                           HttpServletRequest request,
                           Model model) {
        try {
            String currentTheme = themeService.getCurrentTheme(request);
            model.addAttribute("currentTheme", currentTheme != null ? currentTheme : "light");
            model.addAttribute("currentPage", "/cart");

            if (userDetails == null) {
                return "redirect:/login";
            }

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            List<CartItem> cartItems = cartItemRepository.findByUser(user);

            double subtotal = 0.0;
            double totalDiscount = 0.0;
            double finalTotal = 0.0;

            for (CartItem item : cartItems) {
                if (item != null && item.getProduct() != null && item.getProduct().getPrice() != null) {
                    double itemPrice = item.getProduct().getPrice().doubleValue();
                    double itemTotal = itemPrice * item.getQuantity();
                    subtotal += itemTotal;
                    finalTotal += itemTotal;
                }
            }

            double additionalDiscount = 0.0;
            if (finalTotal >= 200000) {
                additionalDiscount = 500.0;
                finalTotal -= additionalDiscount;
                totalDiscount += additionalDiscount;
            }

            double deliveryCost = 0.0;
            if (!cartItems.isEmpty()) {
                deliveryCost = finalTotal >= 200000 ? 0.0 : 300.0;
            }
            finalTotal += deliveryCost;

            model.addAttribute("cartItems", cartItems);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("totalDiscount", totalDiscount);
            model.addAttribute("additionalDiscount", additionalDiscount);
            model.addAttribute("deliveryCost", deliveryCost);
            model.addAttribute("finalTotal", finalTotal);
            model.addAttribute("freeDeliveryThreshold", 200000.0);
            model.addAttribute("hasAdditionalDiscount", additionalDiscount > 0);

            return "cart";

        } catch (Exception e) {
            logger.error("Error loading cart: {}", e.getMessage(), e);

            String currentTheme = themeService.getCurrentTheme(request);
            model.addAttribute("currentTheme", currentTheme != null ? currentTheme : "light");
            model.addAttribute("currentPage", "/cart");

            model.addAttribute("error", "Ошибка при загрузке корзины");
            model.addAttribute("cartItems", new ArrayList<>());
            model.addAttribute("subtotal", 0.0);
            model.addAttribute("totalDiscount", 0.0);
            model.addAttribute("additionalDiscount", 0.0);
            model.addAttribute("deliveryCost", 0.0);
            model.addAttribute("finalTotal", 0.0);
            model.addAttribute("freeDeliveryThreshold", 200000.0);
            model.addAttribute("hasAdditionalDiscount", false);
            return "cart";
        }
    }

    @PostMapping("/cart/update")
    public String updateCartItem(@RequestParam Long itemId,
                                 @RequestParam int quantity,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            Optional<CartItem> cartItemOpt = cartItemRepository.findById(itemId);
            if (cartItemOpt.isPresent()) {
                CartItem cartItem = cartItemOpt.get();

                if (!cartItem.getUser().getEmail().equals(userDetails.getUsername())) {
                    redirectAttributes.addFlashAttribute("error", "Ошибка доступа");
                    return "redirect:/cart";
                }

                Optional<Product> productOpt = productRepository.findById(cartItem.getProduct().getId());
                if (productOpt.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Товар не найден");
                    return "redirect:/cart";
                }

                Product product = productOpt.get();

                if (quantity <= 0) {
                    cartItemRepository.delete(cartItem);
                    redirectAttributes.addFlashAttribute("success", "Товар удален из корзины");
                } else if (quantity > product.getStockQuantity()) {
                    redirectAttributes.addFlashAttribute("error",
                            "Недостаточно товара в наличии. Максимум: " + product.getStockQuantity());
                } else {
                    cartItem.setQuantity(quantity);
                    cartItemRepository.save(cartItem);
                    redirectAttributes.addFlashAttribute("success", "Количество обновлено");
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Товар в корзине не найден");
            }
            return "redirect:/cart";
        } catch (Exception e) {
            logger.error("Error updating cart item: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении корзины");
            return "redirect:/cart";
        }
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long itemId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            Optional<CartItem> cartItemOpt = cartItemRepository.findById(itemId);
            if (cartItemOpt.isPresent()) {
                CartItem cartItem = cartItemOpt.get();

                if (!cartItem.getUser().getEmail().equals(userDetails.getUsername())) {
                    redirectAttributes.addFlashAttribute("error", "Ошибка доступа");
                    return "redirect:/cart";
                }

                String productName = cartItem.getProduct() != null ? cartItem.getProduct().getName() : "Товар";
                cartItemRepository.delete(cartItem);
                redirectAttributes.addFlashAttribute("success", "Товар \"" + productName + "\" удален из корзины");
            } else {
                redirectAttributes.addFlashAttribute("error", "Товар в корзине не найден");
            }
            return "redirect:/cart";
        } catch (Exception e) {
            logger.error("Error removing cart item: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении товара");
            return "redirect:/cart";
        }
    }

    @PostMapping("/cart/clear")
    public String clearCart(@AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            List<CartItem> cartItems = cartItemRepository.findByUser(user);
            cartItemRepository.deleteAll(cartItems);

            redirectAttributes.addFlashAttribute("success", "Корзина полностью очищена");
            return "redirect:/cart";
        } catch (Exception e) {
            logger.error("Error clearing cart: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при очистке корзины");
            return "redirect:/cart";
        }
    }

    @PostMapping("/cart/apply-promo")
    public String applyPromoCode(@RequestParam String promoCode,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            if ("WELCOME10".equals(promoCode.toUpperCase())) {
                redirectAttributes.addFlashAttribute("success", "Промокод применен! Скидка 10% на заказ");
            } else if ("SUMMER2024".equals(promoCode.toUpperCase())) {
                redirectAttributes.addFlashAttribute("success", "Промокод применен! Скидка 15% на летнюю коллекцию");
            } else if ("FREE300".equals(promoCode.toUpperCase())) {
                redirectAttributes.addFlashAttribute("success", "Промокод применен! Скидка 300 ₽ на заказ");
            } else {
                redirectAttributes.addFlashAttribute("error", "Промокод недействителен или истек");
            }

            return "redirect:/cart";
        } catch (Exception e) {
            logger.error("Error applying promo code: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при применении промокода");
            return "redirect:/cart";
        }
    }
}