package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.*;
import com.pneumaliback.www.enums.OrderStatus;
import com.pneumaliback.www.repository.CartRepository;
import com.pneumaliback.www.repository.OrderItemRepository;
import com.pneumaliback.www.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PromotionService promotionService;
    private final DeliveryService deliveryService;
    private final OrderService orderService;
    private final CartService cartService;

    @Transactional
    public Order createOrder(User user, Address shippingAddress, String zone, String promoCode) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Panier introuvable"));
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Panier vide");
        }

        // Construire la commande
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        // Lignes de commande depuis le panier
        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getProduct().getPrice());
            orderItemRepository.save(oi);
            order.getItems().add(oi);
        }

        // Promotion (si code fourni)
        if (promoCode != null && !promoCode.isBlank()) {
            Promotion promo = promotionService.findValidByCode(promoCode).orElse(null);
            if (promo != null) {
                order.setPromotion(promo);
            } else {
                promotionService.resolveFromInfluencerCode(promoCode)
                        .ifPresent(order::setPromotion);
            }
        }
 
        // Livraison (zone + frais)
        BigDecimal fee = deliveryService.quoteShippingFee(zone);
        Delivery delivery = deliveryService.attachDelivery(order, shippingAddress, zone, fee);
        order.setDelivery(delivery);

        // Totaux
        orderService.computeTotals(order);
        orderRepository.save(order);

        // Nettoyer le panier
        cartService.clear(user);

        return order;
    }
}
