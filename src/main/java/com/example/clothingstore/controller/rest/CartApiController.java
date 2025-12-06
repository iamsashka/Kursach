package com.example.clothingstore.controller.rest;

import com.example.clothingstore.model.CartItem;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.User;
import com.example.clothingstore.repository.CartItemRepository;
import com.example.clothingstore.repository.ProductRepository;
import com.example.clothingstore.service.CartService;
import com.example.clothingstore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart API", description = "API для управления корзиной")
public class CartApiController {

    private final CartService cartService;
    private final UserService userService;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Operation(summary = "Получить корзину пользователя")
    @GetMapping
    public ResponseEntity<List<CartItem>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<CartItem> cartItems = cartService.getCartItems(user);
        return ResponseEntity.ok(cartItems);
    }

    @Operation(summary = "Добавить товар в корзину")
    @PostMapping("/add")
    public ResponseEntity<Map<String, String>> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID товара") @RequestParam Long productId,
            @Parameter(description = "Количество") @RequestParam int quantity,
            @Parameter(description = "Размер") @RequestParam(required = false) String size,
            @Parameter(description = "Цвет") @RequestParam(required = false) String color) {

        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Not enough stock available");
        }

        String finalSize = (size != null && !size.trim().isEmpty()) ? size : "M";
        String finalColor = (color != null && !color.trim().isEmpty()) ? color : "Black";

        Optional<CartItem> existingCartItem = cartItemRepository.findByUserAndProductAndSizeAndColor(
                user, product, finalSize, finalColor);

        if (existingCartItem.isPresent()) {
            CartItem cartItem = existingCartItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItemRepository.save(cartItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setSize(finalSize);
            cartItem.setColor(finalColor);
            cartItemRepository.save(cartItem);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Product added to cart successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Удалить товар из корзины")
    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<Map<String, String>> removeFromCart(
            @Parameter(description = "ID элемента корзины") @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        cartItemRepository.delete(cartItem);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Item removed from cart successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Очистить корзину")
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        cartItemRepository.deleteAll(cartItems);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cart cleared successfully");
        return ResponseEntity.ok(response);
    }
}