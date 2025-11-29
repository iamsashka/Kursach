package com.example.clothingstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private List<OrderItemRequest> items;
    private String shippingAddress;
    private String paymentMethod;
    private String customerNotes;
    private String receiptEmail;
    private boolean useProfileEmail = true;

    // Поля для карты
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private String cardHolder;

    @Data
    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;
        private String size;
        private String color;
    }
}