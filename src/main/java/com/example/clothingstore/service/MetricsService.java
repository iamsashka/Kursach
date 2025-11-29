package com.example.clothingstore.service;

import com.example.clothingstore.repository.OrderRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    private final AtomicInteger productsCount = new AtomicInteger(0);
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final OrderRepository orderRepository;

    public MetricsService(MeterRegistry registry, OrderRepository orderRepository) {
        this.orderRepository = orderRepository;

        Gauge.builder("tareno_users_active", activeUsers, AtomicInteger::get)
                .description("Активные пользователи онлайн")
                .register(registry);

        Gauge.builder("tareno_products_count", productsCount, AtomicInteger::get)
                .description("Общее количество товаров")
                .register(registry);

        Gauge.builder("tareno_revenue", totalRevenue, AtomicLong::get)
                .description("Общая выручка магазина")
                .register(registry);
        initializeRevenue();
    }

    public void userLoggedIn() { activeUsers.incrementAndGet(); }
    public void userLoggedOut() { if (activeUsers.get() > 0) activeUsers.decrementAndGet(); }
    public void setProductsCount(int count) { productsCount.set(count); }
    public void addRevenue(double amount) {
        totalRevenue.addAndGet((long) amount);
    }
    @PostConstruct
    public void initializeRevenue() {
        BigDecimal total = orderRepository. sumTotalAmountOfAllOrders();
        totalRevenue.set(total.longValue());
        System.out.println("=== ВОССТАНОВЛЕНА ВЫРУЧКА: " + total + " ===");
    }
}