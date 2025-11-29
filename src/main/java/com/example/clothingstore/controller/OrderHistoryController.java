package com.example.clothingstore.controller;

import com.example.clothingstore.model.User;
import com.example.clothingstore.service.OrderService;
import com.example.clothingstore.service.UserService;
import com.example.clothingstore.service.ThemeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Controller
public class OrderHistoryController {

    private final OrderService orderService;
    private final UserService userService;
    private final ThemeService themeService;

    public OrderHistoryController(OrderService orderService, UserService userService, ThemeService themeService) {
        this.orderService = orderService;
        this.userService = userService;
        this.themeService = themeService;
    }

    @GetMapping("/order-history")
    public String getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            HttpServletRequest request,
            Model model) {

        try {
            // Получаем текущего пользователя
            User currentUser = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Передаем пользователя в модель для использования его настроек
            model.addAttribute("user", currentUser);

            // Передаем текущую тему для навигации
            String currentTheme = themeService.getCurrentTheme(request);
            model.addAttribute("currentTheme", currentTheme);

            // Получаем заказы пользователя с пагинацией
            Pageable pageable = PageRequest.of(page, 10, Sort.by("orderDate").descending());
            var ordersPage = orderService.getUserOrdersPage(currentUser.getId(), pageable);

            model.addAttribute("ordersPage", ordersPage);

            return "order-history";

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка загрузки истории заказов: " + e.getMessage());
            return "order-history";
        }
    }
}