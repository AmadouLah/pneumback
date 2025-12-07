package com.pneumaliback.www.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.QuoteRequestItem;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.QuoteStatus;
import com.pneumaliback.www.repository.ProductRepository;
import com.pneumaliback.www.repository.QuoteRequestItemRepository;
import com.pneumaliback.www.repository.QuoteRequestRepository;
import com.pneumaliback.www.repository.UserRepository;

import com.pneumaliback.www.dto.quote.CreateQuoteRequestPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteRequestService {

    private static final String SEQUENCE_QUOTE_REQUEST = "QUOTE_REQUEST";
    private static final String SEQUENCE_QUOTE = "QUOTE";

    private final QuoteRequestRepository quoteRequestRepository;
    private final QuoteRequestItemRepository quoteRequestItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final NumberSequenceService numberSequenceService;
    private final MailService mailService;
    private final StorageService storageService;
    private final QuotePdfService quotePdfService;
    private final com.pneumaliback.www.repository.AddressRepository addressRepository;

    private static final String QUOTE_STORAGE_FOLDER = "quotes";

    @Transactional
    public QuoteRequest createFromPayload(User user, CreateQuoteRequestPayload payload) {
        Map<Long, Integer> requestedQuantities = aggregateRequestedItems(payload);
        if (requestedQuantities.isEmpty()) {
            throw new IllegalStateException("Votre panier est vide. Ajoutez des produits avant de demander un devis.");
        }

        Map<Long, Product> products = loadProducts(requestedQuantities.keySet());

        QuoteRequest request = new QuoteRequest();
        request.setUser(user);
        request.setRequestNumber(numberSequenceService.nextFormatted(SEQUENCE_QUOTE_REQUEST, "REQ"));
        request.setStatus(QuoteStatus.EN_ATTENTE);
        request.setClientMessage(payload.message());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = products.get(productId);
            if (product == null) {
                throw new IllegalArgumentException("Produit introuvable: " + productId);
            }

            QuoteRequestItem item = createItemFromProduct(request, product, quantity);
            request.getItems().add(item);
            subtotal = subtotal.add(item.getLineTotal());
        }

        request.setSubtotalRequested(subtotal);
        request.setDiscountTotal(BigDecimal.ZERO);
        request.setTotalQuoted(subtotal);

        QuoteRequest savedRequest = quoteRequestRepository.save(request);
        cartService.clear(user);

        mailService.sendQuoteRequestConfirmation(user, savedRequest);
        mailService.notifyAdminsNewQuoteRequest(savedRequest);

        return savedRequest;
    }

    private Map<Long, Integer> aggregateRequestedItems(CreateQuoteRequestPayload payload) {
        if (payload == null || payload.items() == null) {
            return Map.of();
        }

        Map<Long, Integer> aggregated = new LinkedHashMap<>();
        for (CreateQuoteRequestPayload.Item item : payload.items()) {
            if (item == null || item.productId() == null) {
                continue;
            }
            int quantity = item.quantity() != null ? item.quantity() : 0;
            if (quantity <= 0) {
                continue;
            }
            aggregated.merge(item.productId(), quantity, Integer::sum);
        }
        return aggregated;
    }

    private Map<Long, Product> loadProducts(Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    @Transactional(readOnly = true)
    public List<QuoteRequest> listForUser(User user) {
        return quoteRequestRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<QuoteRequest> listForAdmin(List<QuoteStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return quoteRequestRepository.findAllByOrderByCreatedAtDesc();
        }
        return quoteRequestRepository.findByStatusInOrderByCreatedAtDesc(statuses);
    }

    @Transactional(readOnly = true)
    public List<QuoteRequest> listForLivreur(User livreur) {
        return quoteRequestRepository.findByAssignedLivreurOrderByCreatedAtDesc(livreur);
    }

    @Transactional(readOnly = true)
    public QuoteRequest getById(Long id) {
        return loadDetailedQuote(id);
    }

    @Transactional
    public QuoteRequest updateAdminQuote(Long requestId, QuoteAdminUpdate update) {
        QuoteRequest request = loadDetailedQuote(requestId);

        if (update.validUntil() != null) {
            request.setValidUntil(update.validUntil());
        }
        if (update.adminNotes() != null) {
            request.setAdminNotes(update.adminNotes());
        }
        if (update.deliveryDetails() != null) {
            request.setDeliveryDetails(update.deliveryDetails());
        }

        if (update.items() != null && !update.items().isEmpty()) {
            quoteRequestItemRepository.deleteByQuoteRequest(request);
            request.getItems().clear();

            BigDecimal subtotal = BigDecimal.ZERO;
            for (QuoteAdminItem item : update.items()) {
                QuoteRequestItem entity = createItemFromAdminItem(request, item);
                request.getItems().add(entity);
                subtotal = subtotal.add(entity.getLineTotal());
            }
            request.setSubtotalRequested(subtotal);
        }

        updateFinancialFields(request, update);

        if (request.getTotalQuoted() != null && request.getTotalQuoted().compareTo(BigDecimal.ZERO) < 0) {
            request.setTotalQuoted(BigDecimal.ZERO);
        }

        if (request.getStatus() == QuoteStatus.EN_ATTENTE) {
            request.setStatus(QuoteStatus.DEVIS_EN_PREPARATION);
        }

        return quoteRequestRepository.save(request);
    }

    @Transactional
    public QuoteRequest generateAndSendQuote(Long requestId, QuoteAdminUpdate update, String frontendQuoteUrl) {
        QuoteRequest request = updateAdminQuote(requestId, update);

        if (request.getQuoteNumber() == null || request.getQuoteNumber().isBlank()) {
            request.setQuoteNumber(numberSequenceService.nextFormatted(SEQUENCE_QUOTE, "DEV"));
        }

        if (request.getValidUntil() == null) {
            request.setValidUntil(LocalDate.now().plusDays(7));
        }

        User emitter = resolveCurrentAdmin();
        if (emitter != null) {
            loadUserAddresses(emitter);
        }
        byte[] pdf = quotePdfService.generateQuote(request, emitter);
        storeQuotePdf(request, pdf, null);

        request.setStatus(QuoteStatus.DEVIS_ENVOYE);
        QuoteRequest saved = quoteRequestRepository.save(request);

        mailService.sendQuoteReadyEmail(saved.getUser(), saved, frontendQuoteUrl);

        saved.setStatus(QuoteStatus.EN_ATTENTE_VALIDATION);
        return quoteRequestRepository.save(saved);
    }

    @Transactional
    public QuoteRequest validateByClient(Long requestId, String clientIp) {
        QuoteRequest request = loadDetailedQuote(requestId);

        if (request.getStatus() != QuoteStatus.EN_ATTENTE_VALIDATION
                && request.getStatus() != QuoteStatus.DEVIS_ENVOYE) {
            throw new IllegalStateException("Ce devis ne peut pas être validé dans son état actuel.");
        }

        request.setStatus(QuoteStatus.VALIDE_PAR_CLIENT);
        request.setValidatedAt(OffsetDateTime.now());
        request.setValidatedIp(clientIp);

        QuoteRequest saved = quoteRequestRepository.save(request);
        mailService.notifyAdminsQuoteValidated(saved);
        return saved;
    }

    @Transactional
    public QuoteRequest assignLivreur(Long requestId, User livreur, String deliveryDetails) {
        if (livreur == null || livreur.getRole() == null) {
            throw new IllegalArgumentException("Livreur invalide");
        }
        QuoteRequest request = loadDetailedQuote(requestId);

        request.setAssignedLivreur(livreur);
        request.setDeliveryDetails(deliveryDetails != null ? deliveryDetails : request.getDeliveryDetails());
        request.setDeliveryAssignedAt(OffsetDateTime.now());
        request.setStatus(QuoteStatus.EN_COURS_LIVRAISON);

        QuoteRequest saved = quoteRequestRepository.save(request);
        mailService.notifyLivreurAssignment(livreur, saved);
        return saved;
    }

    @Transactional
    public QuoteRequest markDelivered(Long requestId, User livreur) {
        QuoteRequest request = loadDetailedQuote(requestId);

        if (request.getAssignedLivreur() == null
                || !request.getAssignedLivreur().getId().equals(livreur.getId())) {
            throw new IllegalStateException("Ce devis n'est pas assigné à ce livreur.");
        }

        request.setStatus(QuoteStatus.TERMINE);
        request.setDeliveryConfirmedAt(OffsetDateTime.now());

        QuoteRequest saved = quoteRequestRepository.save(request);
        mailService.notifyQuoteDelivered(saved);
        return saved;
    }

    private User resolveCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmailWithAddresses(authentication.getName()).orElse(null);
    }

    public record QuoteAdminItem(Long productId, String productName, String brand, Integer width, Integer profile,
            Integer diameter, Integer quantity, BigDecimal unitPrice) {
    }

    public record QuoteAdminUpdate(List<QuoteAdminItem> items, BigDecimal discountTotal, BigDecimal totalQuoted,
            LocalDate validUntil, String adminNotes, String deliveryDetails) {
    }

    private QuoteRequestItem createItemFromProduct(QuoteRequest request, Product product, int quantity) {
        QuoteRequestItem item = new QuoteRequestItem();
        item.setQuoteRequest(request);
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setBrandName(product.getBrand() != null ? product.getBrand().getName() : null);
        item.setWidthValue(product.getWidth() != null ? product.getWidth().getValue() : null);
        item.setProfileValue(product.getProfile() != null ? product.getProfile().getValue() : null);
        item.setDiameterValue(product.getDiameter() != null ? product.getDiameter().getValue() : null);
        item.setQuantity(quantity);

        BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        item.setUnitPrice(unitPrice);
        item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));

        return item;
    }

    private QuoteRequestItem createItemFromAdminItem(QuoteRequest request, QuoteAdminItem adminItem) {
        QuoteRequestItem item = new QuoteRequestItem();
        item.setQuoteRequest(request);
        item.setProductId(adminItem.productId());
        item.setProductName(adminItem.productName());
        item.setBrandName(adminItem.brand());
        item.setWidthValue(adminItem.width());
        item.setProfileValue(adminItem.profile());
        item.setDiameterValue(adminItem.diameter());
        item.setQuantity(adminItem.quantity());

        BigDecimal unitPrice = adminItem.unitPrice() != null ? adminItem.unitPrice() : BigDecimal.ZERO;
        item.setUnitPrice(unitPrice);
        item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(adminItem.quantity())));

        return item;
    }

    private void updateFinancialFields(QuoteRequest request, QuoteAdminUpdate update) {
        if (update.discountTotal() != null) {
            request.setDiscountTotal(update.discountTotal());
        }

        if (update.totalQuoted() != null) {
            request.setTotalQuoted(update.totalQuoted());
        } else {
            BigDecimal subtotal = calculateSubtotal(request);
            BigDecimal discount = request.getDiscountTotal() != null ? request.getDiscountTotal() : BigDecimal.ZERO;
            request.setTotalQuoted(subtotal.subtract(discount));
        }

        if (request.getTotalQuoted() != null && request.getTotalQuoted().compareTo(BigDecimal.ZERO) < 0) {
            request.setTotalQuoted(BigDecimal.ZERO);
        }
    }

    private BigDecimal calculateSubtotal(QuoteRequest request) {
        return request.getItems().stream()
                .map(QuoteRequestItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public QuoteRequest renderQuotePreview(Long requestId) {
        QuoteRequest request = loadDetailedQuote(requestId);
        User emitter = resolveCurrentAdmin();
        if (emitter != null) {
            loadUserAddresses(emitter);
        }
        byte[] pdf = quotePdfService.generateQuote(request, emitter);
        try {
            storeQuotePdf(request, pdf, "preview");
        } catch (Exception e) {
            log.warn("Impossible de sauvegarder le PDF sur Supabase pour l'aperçu, mais le PDF peut être généré", e);
        }
        return quoteRequestRepository.save(request);
    }

    private void storeQuotePdf(QuoteRequest request, byte[] pdf, String suffix) {
        String existingPath = storageService.extractFilePathFromUrl(request.getQuotePdfUrl());
        if (existingPath != null) {
            storageService.deleteFile(existingPath);
        }

        String rawBaseName = request.getQuoteNumber() != null && !request.getQuoteNumber().isBlank()
                ? request.getQuoteNumber()
                : request.getRequestNumber();
        String baseName = sanitizeFileName(rawBaseName);
        String normalizedSuffix = (suffix != null && !suffix.isBlank()) ? "-" + suffix : "";
        String filename = baseName + normalizedSuffix + ".pdf";

        try {
            String pdfUrl = storageService.uploadBytes(pdf, filename, QUOTE_STORAGE_FOLDER, "application/pdf");
            request.setQuotePdfUrl(pdfUrl);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de sauvegarder le devis sur Supabase", e);
        }
    }

    private String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) {
            return "quote-" + System.currentTimeMillis();
        }
        return input.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private QuoteRequest loadDetailedQuote(Long id) {
        QuoteRequest request = quoteRequestRepository.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Devis introuvable"));

        if (request.getUser() != null) {
            loadUserAddresses(request.getUser());
        }

        try {
            Hibernate.initialize(request.getItems());
        } catch (org.hibernate.LazyInitializationException e) {
            log.warn("Les items du devis {} ne sont pas chargés, chargement explicite", id);
            List<QuoteRequestItem> items = quoteRequestItemRepository.findByQuoteRequest(request);
            if (items != null && !items.isEmpty()) {
                try {
                    request.getItems().clear();
                    request.getItems().addAll(items);
                } catch (Exception ex) {
                    log.error("Erreur lors du chargement des items: {}", ex.getMessage());
                }
            }
        }

        return request;
    }

    public void loadUserAddresses(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        try {
            if (Hibernate.isInitialized(user.getAddresses())) {
                return;
            }
            List<com.pneumaliback.www.entity.Address> addresses = addressRepository.findByUser(user);
            if (addresses != null && !addresses.isEmpty()) {
                user.getAddresses().clear();
                user.getAddresses().addAll(addresses);
            }
        } catch (Exception e) {
            log.debug("Impossible de charger les adresses de l'utilisateur: {}", e.getMessage());
        }
    }
}
