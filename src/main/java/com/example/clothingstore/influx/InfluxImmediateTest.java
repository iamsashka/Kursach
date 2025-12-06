package com.example.clothingstore.influx;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class InfluxImmediateTest {

    private final MeterRegistry meterRegistry;

    public InfluxImmediateTest(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void testInfluxImmediately() {
        System.out.println("=== STARTING INFLUXDB IMMEDIATE TEST ===");

        Counter counter = Counter.builder("influx_immediate_test")
                .tag("test", "immediate")
                .tag("currency", "RUB")
                .tag("source", "postconstruct")
                .register(meterRegistry);

        counter.increment();
        System.out.println("Test counter incremented: influx_immediate_test");

        meterRegistry.counter("direct_immediate_test",
                "type", "direct",
                "currency", "RUB"
        ).increment();

        System.out.println("Direct test metric sent");
        System.out.println("=== INFLUXDB TEST COMPLETE ===");

        try {
            Thread.sleep(6000);
            System.out.println("Metrics should be sent to InfluxDB now");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}