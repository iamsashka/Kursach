package com.example.clothingstore.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manager")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Панель менеджера");
        return "manager/dashboard";
    }

    @GetMapping("/orders")
    public String orderManagement(Model model) {
        model.addAttribute("pageTitle", "Управление заказами");
        return "manager/orders";
    }

    @GetMapping("/products")
    public String productManagement(Model model) {
        model.addAttribute("pageTitle", "Управление товарами");
        return "manager/products";
    }

    @GetMapping("/customers")
    public String customerManagement(Model model) {
        model.addAttribute("pageTitle", "Просмотр клиентов");
        return "manager/customers";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        model.addAttribute("pageTitle", "Статистика");
        return "manager/statistics";
    }
}