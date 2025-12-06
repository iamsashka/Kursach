package com.example.clothingstore.service;

import com.example.clothingstore.repository.OrderRepository;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    private final AtomicInteger productsCount = new AtomicInteger(0);
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final OrderRepository orderRepository;


    private InfluxDBClient influxClient;
    private WriteApiBlocking writeApi;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final String TOKEN = "GBavi4-2OUk9SL6Z-BjTxG215foH5RmDoPgU8KrJURhRbKP4zwiWZylRuk6eTPk126EdTkRyFPuYG_gmzPQY2Q==";
    private static final String ORG = "MPT";
    private static final String BUCKET = "Metrics111";
    private static final String URL = "http://localhost:8086";

    public MetricsService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostConstruct
    public void init() {
        initializeInfluxClient();
        initializeRevenue();
        startMetricsScheduler();
        sendTestMetric();
    }

    private void initializeInfluxClient() {
        try {
            this.influxClient = InfluxDBClientFactory.create(URL, TOKEN.toCharArray());
            this.writeApi = influxClient.getWriteApiBlocking();
            System.out.println("=== INFLUXDB CLIENT INITIALIZED ===");
            System.out.println("URL: " + URL);
            System.out.println("Bucket: " + BUCKET);
            System.out.println("Org: " + ORG);
        } catch (Exception e) {
            System.err.println("Failed to initialize InfluxDB client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startMetricsScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendAllMetrics();
            } catch (Exception e) {
                System.err.println("Error sending metrics to InfluxDB: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void sendAllMetrics() {
        if (writeApi == null) return;

        try {
            Instant now = Instant.now();

            Point usersPoint = Point.measurement("tareno_users_active")
                    .addTag("store_type", "online")
                    .addTag("application", "clothing-store")
                    .addTag("metric_type", "gauge")
                    .addField("value", activeUsers.get())
                    .time(now, WritePrecision.NS);

            Point revenuePoint = Point.measurement("tareno_revenue_total")
                    .addTag("store_type", "online")
                    .addTag("currency", "RUB")
                    .addTag("application", "clothing-store")
                    .addTag("metric_type", "gauge")
                    .addField("value", totalRevenue.get())
                    .time(now, WritePrecision.NS);

            Point productsPoint = Point.measurement("tareno_products_count")
                    .addTag("store_type", "online")
                    .addTag("application", "clothing-store")
                    .addTag("metric_type", "gauge")
                    .addField("value", productsCount.get())
                    .time(now, WritePrecision.NS);

            writeApi.writePoint(BUCKET, ORG, usersPoint);
            writeApi.writePoint(BUCKET, ORG, revenuePoint);
            writeApi.writePoint(BUCKET, ORG, productsPoint);

            System.out.println("=== METRICS SENT TO INFLUXDB ===");
            System.out.println("Active users: " + activeUsers.get());
            System.out.println("Total revenue: " + totalRevenue.get() + " RUB");
            System.out.println("Products count: " + productsCount.get());

        } catch (Exception e) {
            System.err.println("Failed to send metrics: " + e.getMessage());
        }
    }

    private void sendTestMetric() {
        if (writeApi == null) return;

        try {
            Point testPoint = Point.measurement("spring_influx_test")
                    .addTag("application", "clothing-store")
                    .addTag("test", "initialization")
                    .addTag("currency", "RUB")
                    .addField("status", 1)
                    .time(Instant.now(), WritePrecision.NS);

            writeApi.writePoint(BUCKET, ORG, testPoint);
            System.out.println("=== TEST METRIC SENT TO INFLUXDB ===");

        } catch (Exception e) {
            System.err.println("Failed to send test metric: " + e.getMessage());
        }
    }

    public void userLoggedIn() {
        activeUsers.incrementAndGet();
        sendEventMetric("user_login", 1);
    }

    public void userLoggedOut() {
        if (activeUsers.get() > 0) activeUsers.decrementAndGet();
    }

    public void setProductsCount(int count) {
        productsCount.set(count);
    }

    public void addRevenue(double amount) {
        totalRevenue.addAndGet((long) amount);
        sendEventMetric("revenue_added", amount);
    }

    private void sendEventMetric(String event, double value) {
        if (writeApi == null) return;

        try {
            Point eventPoint = Point.measurement("tareno_events")
                    .addTag("event_type", event)
                    .addTag("store_type", "online")
                    .addTag("currency", "RUB")
                    .addTag("application", "clothing-store")
                    .addField("value", value)
                    .time(Instant.now(), WritePrecision.NS);

            writeApi.writePoint(BUCKET, ORG, eventPoint);

        } catch (Exception e) {
            System.err.println("Failed to send event metric: " + e.getMessage());
        }
    }

    @PostConstruct
    public void initializeRevenue() {
        try {
            BigDecimal total = orderRepository.sumTotalAmountOfAllOrders();
            if (total != null) {
                totalRevenue.set(total.longValue());
                System.out.println("=== INITIAL REVENUE: " + total + " RUB ===");
            }
        } catch (Exception e) {
            System.err.println("Error initializing revenue: " + e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
        if (influxClient != null) {
            influxClient.close();
            System.out.println("=== INFLUXDB CLIENT CLOSED ===");
        }
    }
}