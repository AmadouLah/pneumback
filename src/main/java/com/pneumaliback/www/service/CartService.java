package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Cart;
import com.pneumaliback.www.entity.CartItem;
import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.CartItemRepository;
import com.pneumaliback.www.repository.CartRepository;
import com.pneumaliback.www.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Cart getOrCreate(User user) {
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(user);
            return cartRepository.save(c);
        });
    }

    @Transactional
    public Cart addItem(User user, Long productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantité invalide");
        Cart cart = getOrCreate(user);
        Product p = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Produit introuvable"));
        // Chercher si item existe déjà
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst().orElse(null);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(p);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }
        cartRepository.save(cart);
        return cart;
    }

    @Transactional
    public Cart updateItem(User user, Long productId, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantité invalide");
        Cart cart = getOrCreate(user);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst().orElseThrow(() -> new RuntimeException("Article introuvable dans le panier"));
        if (quantity == 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
        }
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart clear(User user) {
        Cart cart = getOrCreate(user);
        cart.getItems().clear();
        return cartRepository.save(cart);
    }
}
