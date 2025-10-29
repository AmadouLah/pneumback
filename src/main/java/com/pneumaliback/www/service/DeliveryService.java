package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.Delivery;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.enums.DeliveryStatus;
import com.pneumaliback.www.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    // Simple quote logic; you can later back this by a Zone table/config
    public BigDecimal quoteShippingFee(String zone) {
        if (zone == null || zone.isBlank()) return BigDecimal.ZERO;
        String z = zone.trim().toLowerCase();
        if (z.contains("bamako")) return new BigDecimal("2000");
        return new BigDecimal("5000");
    }

    @Transactional
    public Delivery attachDelivery(Order order, Address address, String zone, BigDecimal fee) {
        Delivery d = new Delivery();
        d.setOrder(order);
        d.setAddress(address);
        d.setZone(zone);
        d.setShippingFee(fee);
        d.setStatus(DeliveryStatus.PENDING);
        return deliveryRepository.save(d);
    }

    @Transactional
    public Delivery updateStatus(Delivery delivery, DeliveryStatus status) {
        delivery.setStatus(status);
        return deliveryRepository.save(delivery);
    }
}
