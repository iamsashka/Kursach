package com.example.clothingstore.controller;

import com.example.clothingstore.model.Order;
import com.example.clothingstore.model.User;
import com.example.clothingstore.service.CartService;
import com.example.clothingstore.service.FavoriteService;
import com.example.clothingstore.service.OrderService;
import com.example.clothingstore.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class ProfileController {
    private final UserService userService;
    private final CartService cartService;
    private final FavoriteService favoriteService;
    private final OrderService orderService;

    public ProfileController(UserService userService,
                             CartService cartService,
                             FavoriteService favoriteService,
                             OrderService orderService) {
        this.userService = userService;
        this.cartService = cartService;
        this.favoriteService = favoriteService;
        this.orderService = orderService;
    }

    @GetMapping("/profile")
    public String profile(@RequestParam(required = false) Boolean editing,
                          Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        try {
            User user = userService.findByEmail(auth.getName()).orElse(null);
            if (user == null) {
                return "redirect:/login";
            }

            // Загружаем данные профиля
            loadProfileData(user, model);

            // Устанавливаем режим редактирования
            model.addAttribute("editing", Boolean.TRUE.equals(editing));
            model.addAttribute("user", user);

            return "profile";

        } catch (Exception e) {
            // Логируем ошибку для диагностики
            System.err.println("Error loading profile: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/profile?error=load_failed";
        }
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String username,
                                @RequestParam(required = false) String firstName,
                                @RequestParam(required = false) String lastName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String city,
                                @RequestParam(required = false) String postalCode,
                                Authentication authentication) {

        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            User currentUser = userService.findByEmail(authentication.getName()).orElse(null);
            if (currentUser == null) {
                return "redirect:/login";
            }

            // Проверка username - исправленная версия
            if (!username.equals(currentUser.getUsername())) {
                User existingUser = userService.findByUsername(username);
                if (existingUser != null) {
                    return "redirect:/profile?editing=true&error=username_taken";
                }
                currentUser.setUsername(username);
            }

            // Обновляем поля
            currentUser.setFirstName(firstName);
            currentUser.setLastName(lastName);
            currentUser.setPhone(phone);
            currentUser.setAddress(address);
            currentUser.setCity(city);
            currentUser.setPostalCode(postalCode);

            userService.updateProfile(currentUser);
            return "redirect:/profile?success";

        } catch (Exception e) {
            return "redirect:/profile?editing=true&error=update_failed";
        }
    }
    private void loadProfileData(User user, Model model) {
        try {
            // Упрощаем загрузку и устанавливаем значения по умолчанию ДО любых операций
            int cartItemsCount = 0;
            long favoritesCount = 0;
            long ordersCount = 0;
            BigDecimal totalSpent = BigDecimal.ZERO;
            List<Order> recentOrders = new ArrayList<>();

            try {
                cartItemsCount = cartService.getCartItemsCount(user);
            } catch (Exception e) {
                System.err.println("Cart service error: " + e.getMessage());
            }

            try {
                favoritesCount = favoriteService.getUserFavoritesCount(user);
            } catch (Exception e) {
                System.err.println("Favorite service error: " + e.getMessage());
            }

            try {
                ordersCount = orderService.getUserOrdersCount(user.getId());
                totalSpent = orderService.getTotalSpentByUser(user.getId());
                if (totalSpent == null) totalSpent = BigDecimal.ZERO;

                if (model.getAttribute("editing") == null || !Boolean.TRUE.equals(model.getAttribute("editing"))) {
                    recentOrders = orderService.getRecentOrdersByUser(user.getId());
                    if (recentOrders == null) recentOrders = new ArrayList<>();
                }
            } catch (Exception e) {
                System.err.println("Order service error: " + e.getMessage());
            }

            // Гарантированно устанавливаем все атрибуты
            model.addAttribute("cartItemsCount", cartItemsCount);
            model.addAttribute("favoritesCount", favoritesCount);
            model.addAttribute("ordersCount", ordersCount);
            model.addAttribute("totalSpent", totalSpent);
            model.addAttribute("recentOrders", recentOrders);

        } catch (Exception e) {
            // Финальный fallback
            System.err.println("Critical error in loadProfileData: " + e.getMessage());
            model.addAttribute("cartItemsCount", 0);
            model.addAttribute("favoritesCount", 0);
            model.addAttribute("ordersCount", 0);
            model.addAttribute("totalSpent", BigDecimal.ZERO);
            model.addAttribute("recentOrders", new ArrayList<Order>());
        }
    }
}