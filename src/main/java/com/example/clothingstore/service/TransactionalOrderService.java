package com.example.clothingstore.service;

import com.example.clothingstore.dto.OrderRequest;
import com.example.clothingstore.exception.BusinessException;
import com.example.clothingstore.model.*;
import com.example.clothingstore.repository.OrderItemRepository;
import com.example.clothingstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionalOrderService {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Order createOrderFromCart(User user, OrderRequest orderRequest, String receiptEmail) {
        List<CartItem> cartItems = cartService.getCartItems(user);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Корзина пуста");
        }

        // Создаем заказ
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PROCESSING);

        // ВАЖНО: Устанавливаем адрес доставки из формы
        order.setShippingAddress(orderRequest.getShippingAddress());

        // Проверяем, что адрес установлен
        if (orderRequest.getShippingAddress() == null || orderRequest.getShippingAddress().trim().isEmpty()) {
            throw new RuntimeException("Адрес доставки обязателен");
        }

        // ДОБАВЛЯЕМ ТОВАРЫ В ЗАКАЗ
        List<Product> products = cartItems.stream()
                .map(CartItem::getProduct)
                .collect(Collectors.toList());
        order.setProducts(products);

        // Расчет суммы
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // Генерация номера заказа
        order.setOrderNumber("ORD-" + System.currentTimeMillis());

        // Сохраняем заказ
        Order savedOrder = orderRepository.save(order);

        // Создаем OrderItems
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> new OrderItem(
                        savedOrder,
                        cartItem.getProduct(),
                        cartItem.getQuantity(),
                        cartItem.getSize(),
                        cartItem.getColor()
                ))
                .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        // Очищаем корзину
        cartService.clearCart(user);

        System.out.println("Отправляем чек на: " + receiptEmail);

        return savedOrder;
    }
    @Transactional(rollbackFor = Exception.class)
    public Order createOrderWithItems(OrderRequest request, String userEmail) {
        try {
            User user = userService.findByEmail(userEmail)
                    .orElseThrow(() -> new BusinessException("Пользователь не найден"));

            // Создаем заказ
            Order order = new Order();
            order.setUser(user);
            order.setOrderNumber("ORD-" + System.currentTimeMillis());
            order.setStatus(OrderStatus.PENDING);
            order.setOrderDate(LocalDateTime.now());

            // Рассчитываем общую сумму
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<Product> products = new ArrayList<>();

            for (OrderRequest.OrderItemRequest item : request.getItems()) {
                Product product = productService.getProductById(item.getProductId());

                // Проверяем доступность
                if (!productService.isProductAvailable(product.getId(), item.getQuantity())) {
                    throw new BusinessException("Товар '" + product.getName() + "' недоступен в количестве " + item.getQuantity());
                }

                // Обновляем остатки
                productService.updateStockQuantity(product.getId(), item.getQuantity());

                // Добавляем к общей сумме
                totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                products.add(product);
            }

            order.setProducts(products);
            order.setTotalAmount(totalAmount);

            return orderService.saveOrder(order);

        } catch (Exception e) {
            throw new BusinessException("Ошибка создания заказа: " + e.getMessage());
        }
    }
}