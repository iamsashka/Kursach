package com.example.clothingstore.controller.rest;

import com.example.clothingstore.model.Order;
import com.example.clothingstore.model.OrderStatus;
import com.example.clothingstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class OrderApiController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<Order>> getOrders(
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
            orderPage = orderService.searchOrders(search, pageable);
        } else if (status != null) {
            orderPage = orderService.getOrdersByStatus(status, pageable);
        } else {
            orderPage = orderService.getAllOrders(pageable);
        }

        return ResponseEntity.ok(orderPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order savedOrder = orderService.saveOrder(order);
        return ResponseEntity.ok(savedOrder);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        order.setId(id);
        Order updatedOrder = orderService.saveOrder(order);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        OrderStatus newStatus = OrderStatus.valueOf(request.get("status"));
        Order order = orderService.getOrderById(id);
        order.setStatus(newStatus);
        Order updatedOrder = orderService.saveOrder(order);
        return ResponseEntity.ok(updatedOrder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.softDeleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteOrder(@PathVariable Long id) {
        orderService.hardDeleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}