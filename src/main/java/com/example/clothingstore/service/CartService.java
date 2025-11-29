package com.example.clothingstore.service;

import com.example.clothingstore.model.CartItem;
import com.example.clothingstore.model.User;
import com.example.clothingstore.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;

    public int getCartItemsCount(User user) {
        return cartItemRepository.countByUser(user);
    }
    public void clearCart(User user) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        cartItemRepository.deleteAll(cartItems);
    }

    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }
}
