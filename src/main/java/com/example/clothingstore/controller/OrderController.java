package com.example.clothingstore.controller;

import com.example.clothingstore.model.Order;
import com.example.clothingstore.model.User;
import com.example.clothingstore.model.OrderStatus;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.service.MetricsService;
import com.example.clothingstore.service.OrderService;
import com.example.clothingstore.service.UserService;
import com.example.clothingstore.service.ProductService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/orders")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final MetricsService metricsService;

    public OrderController(OrderService orderService, UserService userService, ProductService productService, MetricsService metricsService) {
        this.orderService = orderService;
        this.userService = userService;
        this.productService = productService;
        this.metricsService = metricsService;
    }

    @GetMapping
    public String listOrders(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(defaultValue = "orderDate") String sortBy,
                             @RequestParam(defaultValue = "desc") String sortDir,
                             @RequestParam(required = false) String search,
                             @RequestParam(required = false) OrderStatus status) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Order> orderPage;
        if (search != null && !search.isBlank()) {
            orderPage = orderService.searchOrdersWithDetails(search, pageable);
        } else if (status != null) {
            orderPage = orderService.getOrdersByStatusWithDetails(status, pageable);
        } else {
            orderPage = orderService.getAllOrdersWithDetails(pageable);
        }

        long pendingCount = orderService.countOrdersByStatus(OrderStatus.PENDING);
        long deliveredCount = orderService.countOrdersByStatus(OrderStatus.DELIVERED);

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentPage", page);
        model.addAttribute("allStatuses", OrderStatus.values());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("deliveredCount", deliveredCount);

        return "orders/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("order", new Order());
        List<User> users = userService.getAllActiveUsers();
        List<Product> products = productService.getAllActiveProducts();
        model.addAttribute("users", users);
        model.addAttribute("products", products);
        return "orders/form";
    }
    @GetMapping("/export/excel")
    public void exportToExcel(HttpServletResponse response,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) OrderStatus status) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=orders_" + LocalDate.now() + ".xlsx";
        response.setHeader(headerKey, headerValue);

        List<Order> orders;
        if (search != null && !search.isBlank()) {
            orders = orderService.searchOrdersWithDetails(search);
        } else if (status != null) {
            orders = orderService.getOrdersByStatusWithDetails(status);
        } else {
            orders = orderService.getAllOrdersWithDetails();
        }

        orderService.exportToExcel(orders, response);
    }

    @GetMapping("/export/pdf")
    public void exportToPdf(HttpServletResponse response,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) OrderStatus status) throws IOException {

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=orders_" + LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);

        List<Order> orders;
        if (search != null && !search.isBlank()) {
            orders = orderService.searchOrdersWithDetails(search);
        } else if (status != null) {
            orders = orderService.getOrdersByStatusWithDetails(status);
        } else {
            orders = orderService.getAllOrdersWithDetails();
        }

        orderService.exportToPdf(orders, response);
    }
    @GetMapping("/archive")
    public String showArchive(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> archivedOrders = orderService.getArchivedOrders(pageable);

        model.addAttribute("archivedOrders", archivedOrders);
        model.addAttribute("currentPage", page);
        return "orders/archive";
    }
    @PostMapping("/create")
    public String createOrder(@Valid @ModelAttribute Order order,
                              BindingResult bindingResult,
                              @RequestParam("user") Long userId,
                              @RequestParam List<Long> productIds,
                              @RequestParam(defaultValue = "1") List<Integer> quantities, // ДОБАВИТЬ
                              Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userService.getAllActiveUsers());
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("allStatuses", OrderStatus.values()); // ДОБАВИТЬ если нужно
            return "orders/form";
        }

        try {
            User user = userService.findById(userId);
            order.setUser(user);

            List<Product> products = productService.findByIds(productIds);
            order.setProducts(products);

            // Считаем все с учетом количестваааа
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                Integer quantity = quantities.get(i);
                totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            }
            order.setTotalAmount(totalAmount);

            // Генерим номерок заказа
            if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
                order.setOrderNumber("ORD-" + System.currentTimeMillis());
            }

            orderService.saveOrder(order);
            //Метрика номер 3333
            metricsService.addRevenue(totalAmount.doubleValue());
            return "redirect:/orders?success=Order+created+successfully";

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка создания заказа: " + e.getMessage());
            model.addAttribute("users", userService.getAllActiveUsers());
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("allStatuses", OrderStatus.values()); // ДОБАВИТЬ если нужно
            return "orders/form";
        }
    }
    @GetMapping("/restore/{id}")
    public String restoreOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.restoreOrder(id);
            redirectAttributes.addFlashAttribute("success", "Заказ восстановлен из архива");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка восстановления заказа: " + e.getMessage());
        }
        return "redirect:/orders/archive";
    }
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("users", userService.getAllActiveUsers());
        model.addAttribute("products", productService.getAllActiveProducts());
        return "orders/form";
    }

    @PostMapping("/update/{id}")
    public String updateOrder(@PathVariable Long id,
                              @Valid @ModelAttribute Order order,
                              BindingResult bindingResult,
                              @RequestParam("user") Long userId,
                              @RequestParam List<Long> productIds,
                              Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userService.getAllActiveUsers());
            model.addAttribute("products", productService.getAllActiveProducts());
            return "orders/form";
        }

        try {
            Order existingOrder = orderService.getOrderById(id);

            User user = userService.findById(userId);
            existingOrder.setUser(user);

            List<Product> products = productService.findByIds(productIds);
            existingOrder.setProducts(products);

            // Update fields
            existingOrder.setOrderNumber(order.getOrderNumber());
            existingOrder.setShippingAddress(order.getShippingAddress());
            existingOrder.setStatus(order.getStatus());

            // Recalculate total amount
            BigDecimal totalAmount = products.stream()
                    .map(Product::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            existingOrder.setTotalAmount(totalAmount);

            orderService.saveOrder(existingOrder);
            return "redirect:/orders?success=Order+updated+successfully";

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка обновления заказа: " + e.getMessage());
            model.addAttribute("users", userService.getAllActiveUsers());
            model.addAttribute("products", productService.getAllActiveProducts());
            return "orders/form";
        }
    }

    @PostMapping("/update-status")
    public String updateOrderStatus(@RequestParam Long orderId,
                                    @RequestParam OrderStatus newStatus,
                                    @RequestParam(required = false) String comment,
                                    RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.getOrderById(orderId);
            order.setStatus(newStatus);
            orderService.saveOrder(order);

            redirectAttributes.addFlashAttribute("success", "Статус заказа успешно обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления статуса: " + e.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/soft-delete/{id}")
    public String softDeleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.softDeleteOrder(id);
            redirectAttributes.addFlashAttribute("success", "Заказ перемещен в архив");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка архивации заказа: " + e.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/hard-delete/{id}")
    public String hardDeleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.hardDeleteOrder(id);
            redirectAttributes.addFlashAttribute("success", "Заказ полностью удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка полного удаления заказа: " + e.getMessage());
        }
        return "redirect:/orders";
    }
}