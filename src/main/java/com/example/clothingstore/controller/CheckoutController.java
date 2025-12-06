package com.example.clothingstore.controller;

import com.example.clothingstore.dto.OrderRequest;
import com.example.clothingstore.model.CartItem;
import com.example.clothingstore.model.User;
import com.example.clothingstore.service.CartService;
import com.example.clothingstore.service.MetricsService;
import com.example.clothingstore.service.TransactionalOrderService;
import com.example.clothingstore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final UserService userService;
    private final CartService cartService;
    private final TransactionalOrderService orderService;
    private final MetricsService metricsService;

    @GetMapping
    public String checkoutPage(Authentication authentication, Model model) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<CartItem> cartItems = cartService.getCartItems(user);

        if (cartItems.isEmpty()) {
            return "redirect:/cart?error=Корзина пуста";
        }

        double subtotal = calculateSubtotal(cartItems);
        double deliveryCost = subtotal >= 200000 ? 0 : 300;
        double finalTotal = subtotal + deliveryCost;

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUseProfileEmail(true);

        model.addAttribute("orderRequest", orderRequest);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("user", user);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("deliveryCost", deliveryCost);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("title", "Оформление заказа");
        return "checkout";
    }

    @PostMapping("/place-order")
    public String placeOrder(@ModelAttribute OrderRequest orderRequest,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            String receiptEmail = orderRequest.isUseProfileEmail()
                    ? user.getEmail()
                    : orderRequest.getReceiptEmail();

            orderService.createOrderFromCart(user, orderRequest, receiptEmail);

            List<CartItem> cartItems = cartService.getCartItems(user);
            double subtotal = calculateSubtotal(cartItems);
            metricsService.addRevenue(subtotal);

            redirectAttributes.addFlashAttribute("success",
                    "Заказ успешно создан! Чек отправлен на " + receiptEmail);
            return "redirect:/order-history";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании заказа: " + e.getMessage());
            return "redirect:/checkout";
        }
    }
    private double calculateFinalTotal(User user) {
        List<CartItem> cartItems = cartService.getCartItems(user);
        double subtotal = calculateSubtotal(cartItems);
        double deliveryCost = subtotal >= 200000 ? 0 : 300;
        return subtotal + deliveryCost;
    }
    private double calculateSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .mapToDouble(item -> {
                    if (item != null && item.getProduct() != null && item.getProduct().getPrice() != null) {
                        return item.getProduct().getPrice().doubleValue() * item.getQuantity();
                    }
                    return 0.0;
                })
                .sum();
    }
}