package com.pneumaliback.www.service;

import com.pneumaliback.www.enums.PaymentMethod;
import com.pneumaliback.www.enums.PaymentStatus;
import com.pneumaliback.www.entity.Payment;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.repository.PaymentRepository;
import com.pneumaliback.www.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final EntityManager entityManager;

    // Order chosen to prioritize user experience in Mali
    private static final List<PaymentMethod> ORDERED_METHODS = List.of(
            PaymentMethod.ORANGE_MONEY,
            PaymentMethod.MALITEL_MONEY,
            PaymentMethod.MOOV_MONEY,
            PaymentMethod.BANK_CARD,
            PaymentMethod.PAYPAL,
            PaymentMethod.CASH_ON_DELIVERY);

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

    @Transactional
    public Payment createPayment(Order order, PaymentMethod method, BigDecimal amount, String invoiceToken) {
        // L'Order est déjà persisté et attaché à la session Hibernate dans la même
        // transaction
        // On utilise getReferenceById pour obtenir une référence gérée par Hibernate
        Order attachedOrder = orderRepository.getReferenceById(order.getId());

        Payment payment = new Payment();
        payment.setOrder(attachedOrder);
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setProvider("Paydunya");
        payment.setInvoiceToken(invoiceToken);
        Payment savedPayment = paymentRepository.save(payment);
        // Forcer le flush pour que le paiement soit immédiatement visible dans d'autres
        // transactions
        entityManager.flush();
        log.debug("Paiement créé avec invoiceToken: {}", savedPayment.getInvoiceToken());
        return savedPayment;
    }

    @Transactional
    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status, String transactionReference) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Paiement introuvable"));
        payment.setStatus(status);
        if (transactionReference != null && !transactionReference.isBlank()) {
            payment.setTransactionReference(transactionReference);
        }
        return paymentRepository.save(payment);
    }

    public void confirmSuccessByTransaction(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank())
            return;
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
