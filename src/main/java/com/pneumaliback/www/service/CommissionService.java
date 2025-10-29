package com.pneumaliback.www.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.pneumaliback.www.entity.Commission;
import com.pneumaliback.www.entity.Influenceur;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.enums.CommissionStatus;
import com.pneumaliback.www.repository.CommissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRepository commissionRepository;

    public void createIfEligible(Order order) {
        if (order == null) return;
        Promotion promo = order.getPromotion();
        if (promo == null) return;
        Influenceur inf = promo.getInfluenceur();
        if (inf == null) return;

        Optional<Commission> existing = commissionRepository.findByOrder(order);
        if (existing.isPresent()) return; // idempotent

        BigDecimal base = defaultZero(order.getTotalAmount());
        BigDecimal rate = defaultZero(inf.getCommissionRate());
        BigDecimal amount = base.multiply(rate).divide(BigDecimal.valueOf(100));
        if (amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;

        Commission c = new Commission();
        c.setOrder(order);
        c.setInfluenceur(inf);
        c.setBaseAmount(base);
        c.setRate(rate);
        c.setAmount(amount);
        c.setStatus(CommissionStatus.PENDING);
        c.setPaidAt(null);
        commissionRepository.save(c);
    }

    public void markPaid(Commission commission) {
        if (commission == null) return;
        commission.setStatus(CommissionStatus.PAID);
        commission.setPaidAt(LocalDateTime.now());
        commissionRepository.save(commission);
    }

    private BigDecimal defaultZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
