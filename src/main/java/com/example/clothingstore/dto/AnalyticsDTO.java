package com.example.clothingstore.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
public class AnalyticsDTO {
    private LocalDate startDate;
    private LocalDate endDate;

    private Long totalUsers = 0L;
    private Long totalOrders = 0L;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal averageOrderValue = BigDecimal.ZERO;
    private Double conversionRate = 0.0;

    private Double userGrowthRate = 0.0;
    private Double orderGrowthRate = 0.0;
    private Double revenueGrowthRate = 0.0;

    private Map<String, Long> ordersByCategory = new HashMap<>();
    private Map<String, BigDecimal> revenueByCategory = new HashMap<>();
    private Map<String, Long> topSellingProducts = new HashMap<>();

    private Map<String, Long> dailyOrders = new HashMap<>();
    private Map<String, BigDecimal> dailyRevenue = new HashMap<>();
    private Map<String, Long> productIds = new HashMap<>();

    public Long getProductIdByName(String productName) {
        return productIds.getOrDefault(productName, 1L);
    }
}