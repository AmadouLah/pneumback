package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.entity.OrderItem;
import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.enums.PromotionType;
import com.pneumaliback.www.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CommissionService commissionService;

    public void computeTotals(Order order) {
        // Subtotal = sum(qty * unitPrice)
        BigDecimal subtotal = order.getItems().stream()
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Discount from promotion (if any)
        BigDecimal discount = computeDiscount(subtotal, order.getPromotion());

        // Shipping fee from delivery (if any)
        BigDecimal shipping = order.getDelivery() != null && order.getDelivery().getShippingFee() != null
                ? order.getDelivery().getShippingFee()
                : BigDecimal.ZERO;

        // Total = subtotal - discount + shipping
        BigDecimal total = subtotal.subtract(discount).add(shipping);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO; // never negative
        }

        order.setSubtotal(subtotal);
        order.setDiscountTotal(discount);
        order.setShippingFee(shipping);
        order.setTotalAmount(total);
    }

    private BigDecimal lineTotal(OrderItem item) {
        if (item.getUnitPrice() == null) return BigDecimal.ZERO;
        BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
        return item.getUnitPrice().multiply(qty);
    }

    private BigDecimal computeDiscount(BigDecimal subtotal, Promotion promo) {
        if (promo == null) return BigDecimal.ZERO;
        PromotionType type = promo.getType();
        if (type == null) return BigDecimal.ZERO;

        switch (type) {
            case PERCENTAGE: {
                if (promo.getDiscountPercentage() == null) return BigDecimal.ZERO;
                BigDecimal percent = promo.getDiscountPercentage();
                BigDecimal value = subtotal.multiply(percent).divide(BigDecimal.valueOf(100));
                return clampDiscount(subtotal, value);
            }
            case FIXED_AMOUNT: {
                if (promo.getDiscountAmount() == null) return BigDecimal.ZERO;
                return clampDiscount(subtotal, promo.getDiscountAmount());
            }
            case BUY_ONE_GET_ONE: {
                // Minimal implementation: apply zero here; implement line-level BOGO in pricing engine if needed
                return BigDecimal.ZERO;
            }
            case INFLUENCER_CODE: {
                // Traitement via pourcentage/montant déjà saisi; si aucun, 0
                BigDecimal viaPercent = promo.getDiscountPercentage() != null
                        ? subtotal.multiply(promo.getDiscountPercentage()).divide(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                BigDecimal viaAmount = Objects.requireNonNullElse(promo.getDiscountAmount(), BigDecimal.ZERO);
                BigDecimal value = viaPercent.max(viaAmount);
                return clampDiscount(subtotal, value);
            }
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal clampDiscount(BigDecimal base, BigDecimal discount) {
        if (discount == null) return BigDecimal.ZERO;
        if (discount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (discount.compareTo(base) > 0) return base;
        return discount;
    }

    public void confirm(Order order) {
        if (order == null) return;
        order.setStatus(OrderStatus.CONFIRMED);
        computeTotals(order);
        commissionService.createIfEligible(order);
        // Persistence is delegated to the caller to avoid redundant saves
    }
}
