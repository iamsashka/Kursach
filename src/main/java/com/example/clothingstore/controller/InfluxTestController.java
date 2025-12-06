package com.example.clothingstore.controller;

import com.example.clothingstore.service.MetricsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/influx-test")
public class InfluxTestController {

    private final MetricsService metricsService;

    public InfluxTestController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @PostMapping("/user-login")
    public String testUserLogin() {
        metricsService.userLoggedIn();
        return "User login event sent to InfluxDB!";
    }

    @PostMapping("/add-revenue/{amount}")
    public String testAddRevenue(@PathVariable double amount) {
        metricsService.addRevenue(amount);
        return "Revenue added: " + amount + " RUB (sent to InfluxDB)";
    }

    @PostMapping("/set-products/{count}")
    public String testSetProducts(@PathVariable int count) {
        metricsService.setProductsCount(count);
        return "Products count set to: " + count + " (sent to InfluxDB)";
    }

    @GetMapping("/status")
    public String getStatus() {
        return "InfluxDB Metrics Service is running";
    }
}