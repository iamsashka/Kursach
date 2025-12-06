package com.example.clothingstore.service;

import com.example.clothingstore.config.AnalyticsConfig;
import com.example.clothingstore.dto.AnalyticsDTO;
import com.example.clothingstore.model.OrderStatus;
import com.example.clothingstore.repository.OrderItemRepository;
import com.example.clothingstore.repository.OrderRepository;
import com.example.clothingstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnalyticsService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AnalyticsConfig analyticsConfig;

    public AnalyticsDTO getDashboardAnalytics(LocalDate startDate, LocalDate endDate) {
        AnalyticsDTO analytics = new AnalyticsDTO();

        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            LocalDate previousStartDate = startDate.minusMonths(1);
            LocalDate previousEndDate = endDate.minusMonths(1);
            LocalDateTime previousStartDateTime = previousStartDate.atStartOfDay();
            LocalDateTime previousEndDateTime = previousEndDate.atTime(23, 59, 59);

            // 1. ПОЛЬЗОВАТЕЛИ
            Long currentUsers = userRepository.countByCreatedAtBetween(startDateTime, endDateTime);
            Long previousUsers = userRepository.countByCreatedAtBetween(previousStartDateTime, previousEndDateTime);
            analytics.setTotalUsers(currentUsers != null ? currentUsers : 0L);
            analytics.setUserGrowthRate(calculateGrowthRate(currentUsers, previousUsers));

            // 2. ЗАКАЗЫ
            Long currentOrders = orderRepository.countByOrderDateBetweenAndStatusIn(
                    startDateTime, endDateTime, analyticsConfig.getOrderCountStatuses());
            Long previousOrders = orderRepository.countByOrderDateBetweenAndStatusIn(
                    previousStartDateTime, previousEndDateTime, analyticsConfig.getOrderCountStatuses());
            analytics.setTotalOrders(currentOrders != null ? currentOrders : 0L);
            analytics.setOrderGrowthRate(calculateGrowthRate(currentOrders, previousOrders));

            // 3. ВЫРУЧКА
            BigDecimal currentRevenue = orderRepository.calculateRevenueByStatusIn(
                    startDateTime, endDateTime, analyticsConfig.getRevenueStatuses());
            BigDecimal previousRevenue = orderRepository.calculateRevenueByStatusIn(
                    previousStartDateTime, previousEndDateTime, analyticsConfig.getRevenueStatuses());
            analytics.setTotalRevenue(currentRevenue != null ? currentRevenue : BigDecimal.ZERO);
            analytics.setRevenueGrowthRate(calculateGrowthRate(currentRevenue, previousRevenue));

            // 4. СРЕДНИЙ ЧЕК
            if (currentOrders != null && currentOrders > 0 && currentRevenue != null) {
                analytics.setAverageOrderValue(
                        currentRevenue.divide(BigDecimal.valueOf(currentOrders), 2, RoundingMode.HALF_UP)
                );
            } else {
                analytics.setAverageOrderValue(BigDecimal.ZERO);
            }

            // 5. КОНВЕРСИЯ
            if (currentUsers != null && currentUsers > 0 && currentOrders != null) {
                double conversion = (currentOrders.doubleValue() / currentUsers.doubleValue()) * 100;
                analytics.setConversionRate(Math.min(conversion, 100.0));
            } else {
                analytics.setConversionRate(0.0);
            }

            // 6. ДАННЫЕ ДЛЯ ГРАФИКОВ
            analytics.setRevenueByCategory(getRevenueByCategory(startDateTime, endDateTime));
            analytics.setDailyOrders(getDailyOrders(startDateTime, endDateTime));
            analytics.setTopSellingProducts(getTopSellingProducts(startDateTime, endDateTime));

            log.info("Analytics calculated: users={}, orders={}, revenue={}",
                    analytics.getTotalUsers(), analytics.getTotalOrders(), analytics.getTotalRevenue());

        } catch (Exception e) {
            log.error("Error calculating analytics", e);
            setDefaultValues(analytics);
        }

        return analytics;
    }

    private Map<String, BigDecimal> getRevenueByCategory(LocalDateTime start, LocalDateTime end) {
        try {
            List<Object[]> results = orderItemRepository.getRevenueByCategory(start, end);
            Map<String, BigDecimal> revenueMap = new HashMap<>();

            for (Object[] result : results) {
                if (result.length >= 2 && result[0] != null && result[1] != null) {
                    String category = result[0].toString();
                    BigDecimal revenue = (BigDecimal) result[1];
                    revenueMap.put(category, revenue);
                }
            }

            log.info("Revenue by category: {}", revenueMap);
            return revenueMap;

        } catch (Exception e) {
            log.error("Error getting revenue by category", e);
            return new HashMap<>();
        }
    }

    private Map<String, Long> getDailyOrders(LocalDateTime start, LocalDateTime end) {
        try {
            List<Object[]> results = orderRepository.getDailyOrderCounts(start, end);
            Map<String, Long> dailyOrders = new HashMap<>();

            for (Object[] result : results) {
                if (result.length >= 2 && result[0] != null && result[1] != null) {
                    String date = result[0].toString(); // Формат: 2024-01-01
                    Long count = ((Number) result[1]).longValue();
                    dailyOrders.put(date, count);
                }
            }

            log.info("Daily orders: {}", dailyOrders);
            return dailyOrders;

        } catch (Exception e) {
            log.error("Error getting daily orders", e);
            return new HashMap<>();
        }
    }

    private Map<String, Long> getTopSellingProducts(LocalDateTime start, LocalDateTime end) {
        try {
            List<Object[]> results = orderItemRepository.findTopSellingProducts(start, end);
            Map<String, Long> topProducts = new HashMap<>();

            for (Object[] result : results) {
                if (result.length >= 2 && result[0] != null && result[1] != null) {
                    String productName = result[0].toString();
                    Long quantity = ((Number) result[1]).longValue();
                    topProducts.put(productName, quantity);
                }
            }

            log.info("Top products: {}", topProducts);
            return topProducts;

        } catch (Exception e) {
            log.error("Error getting top products", e);
            return new HashMap<>();
        }
    }

    private Double calculateGrowthRate(Number current, Number previous) {
        if (current == null || previous == null) return 0.0;

        double currentVal = current.doubleValue();
        double previousVal = previous.doubleValue();

        if (previousVal == 0) {
            return currentVal > 0 ? 100.0 : 0.0;
        }

        return ((currentVal - previousVal) / previousVal) * 100;
    }

    private Double calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return 0.0;

        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }

        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private void setDefaultValues(AnalyticsDTO analytics) {
        analytics.setTotalUsers(0L);
        analytics.setTotalOrders(0L);
        analytics.setTotalRevenue(BigDecimal.ZERO);
        analytics.setAverageOrderValue(BigDecimal.ZERO);
        analytics.setConversionRate(0.0);
        analytics.setUserGrowthRate(0.0);
        analytics.setOrderGrowthRate(0.0);
        analytics.setRevenueGrowthRate(0.0);
        analytics.setRevenueByCategory(new HashMap<>());
        analytics.setDailyOrders(new HashMap<>());
        analytics.setTopSellingProducts(new HashMap<>());
    }

    public String exportToCsv(AnalyticsDTO analytics) {
        StringBuilder csv = new StringBuilder();
        csv.append("Метрика,Значение,Рост(%)\n");
        csv.append(String.format("Пользователи,%d,%.1f\n",
                analytics.getTotalUsers(), analytics.getUserGrowthRate()));
        csv.append(String.format("Заказы,%d,%.1f\n",
                analytics.getTotalOrders(), analytics.getOrderGrowthRate()));
        csv.append(String.format("Выручка,%.2f,%.1f\n",
                analytics.getTotalRevenue(), analytics.getRevenueGrowthRate()));
        csv.append(String.format("Средний чек,%.2f,-\n",
                analytics.getAverageOrderValue()));
        csv.append(String.format("Конверсия,%.1f%%,-\n",
                analytics.getConversionRate()));

        csv.append("\nВыручка по категориям:\n");
        analytics.getRevenueByCategory().forEach((category, revenue) ->
                csv.append(String.format("%s,%.2f\n", category, revenue))
        );

        return csv.toString();
    }
}