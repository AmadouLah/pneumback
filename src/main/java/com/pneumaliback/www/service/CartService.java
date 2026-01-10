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

import java.util.Map;

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

    @Transactional
    public Cart syncCartItems(User user, Map<Long, Integer> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Le panier ne peut pas être vide");
        }
        
        Cart cart = getOrCreate(user);
        
        // Supprimer les anciens items du panier
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cartRepository.flush(); // S'assurer que la suppression est persistée
        
        // Ajouter les nouveaux items
        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            if (quantity != null && quantity > 0) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Produit introuvable: " + productId));
                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setQuantity(quantity);
                cart.getItems().add(item);
            }
        }
        
        return cartRepository.save(cart);
    }
}
