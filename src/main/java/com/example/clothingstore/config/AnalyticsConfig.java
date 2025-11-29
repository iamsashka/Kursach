package com.example.clothingstore.config;

import com.example.clothingstore.model.OrderStatus;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "analytics")
public class AnalyticsConfig {

    private List<OrderStatus> revenueStatuses = Arrays.asList(
            OrderStatus.DELIVERED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED
    );

    private List<OrderStatus> orderCountStatuses = Arrays.asList(
            OrderStatus.PENDING,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    public boolean isIncludedInRevenue(OrderStatus status) {
        return revenueStatuses.contains(status);
    }

    public boolean isIncludedInOrderCount(OrderStatus status) {
        return orderCountStatuses.contains(status);
    }
}