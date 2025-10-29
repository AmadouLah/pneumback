package com.pneumaliback.www.service;

import com.pneumaliback.www.enums.PaymentMethod;
import com.pneumaliback.www.enums.PaymentStatus;
import com.pneumaliback.www.entity.Payment;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.repository.PaymentRepository;
import com.pneumaliback.www.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    // Order chosen to prioritize user experience in Mali
    private static final List<PaymentMethod> ORDERED_METHODS = List.of(
            PaymentMethod.ORANGE_MONEY,
            PaymentMethod.MALITEL_MONEY,
            PaymentMethod.MOOV_MONEY,
            PaymentMethod.BANK_CARD,
            PaymentMethod.PAYPAL,
            PaymentMethod.CASH_ON_DELIVERY
    );

    public List<PaymentMethod> getOrderedPaymentMethods() {
        // In case enum gains new values later, append unknowns at the end
        List<PaymentMethod> all = Arrays.asList(PaymentMethod.values());
        List<PaymentMethod> ordered = ORDERED_METHODS.stream()
                .filter(all::contains)
                .collect(Collectors.toList());
        // Add any methods not explicitly ordered
        all.stream()
                .filter(m -> !ordered.contains(m))
                .forEach(ordered::add);
        return ordered;
    }

    public List<String> getOrderedPaymentMethodNames() {
        return getOrderedPaymentMethods().stream()
                .map(PaymentMethod::getDisplayName)
                .collect(Collectors.toList());
    }

    public void confirmSuccessByTransaction(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank()) return;
        paymentRepository.findByTransactionReference(transactionReference).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.SUCCESS);
            Payment savedPayment = paymentRepository.save(payment);
            Order order = savedPayment.getOrder();
            if (order != null) {
                orderService.confirm(order);
                orderRepository.save(order);
            }
        });
    }
}
