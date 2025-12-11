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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.QuoteRequestItem;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.QuoteStatus;
import com.pneumaliback.www.enums.Role;
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
    private final com.pneumaliback.www.repository.DeliveryProofRepository deliveryProofRepository;

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

        // Validation : Si le devis est validé par le client ou dans un stade supérieur,
        // personne ne peut plus modifier le devis (ni admin, ni développeur)
        boolean isQuoteValidatedOrBeyond = request.getStatus() == QuoteStatus.VALIDE_PAR_CLIENT 
                || request.getStatus() == QuoteStatus.EN_COURS_LIVRAISON
                || request.getStatus() == QuoteStatus.LIVRE_EN_ATTENTE_CONFIRMATION
                || request.getStatus() == QuoteStatus.CLIENT_ABSENT
                || request.getStatus() == QuoteStatus.TERMINE;

        if (isQuoteValidatedOrBeyond) {
            throw new IllegalStateException(
                    "Impossible de modifier ce devis : une fois validé par le client, aucune modification n'est autorisée.");
        }

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
        // Vérifier le statut AVANT d'appliquer les mises à jour
        QuoteRequest currentRequest = loadDetailedQuote(requestId);

        // Vérifier si le devis a déjà été envoyé (statuts indiquant un envoi précédent)
        if (currentRequest.getStatus() == QuoteStatus.DEVIS_ENVOYE 
                || currentRequest.getStatus() == QuoteStatus.EN_ATTENTE_VALIDATION
                || currentRequest.getStatus() == QuoteStatus.VALIDE_PAR_CLIENT
                || currentRequest.getStatus() == QuoteStatus.EN_COURS_LIVRAISON
                || currentRequest.getStatus() == QuoteStatus.LIVRE_EN_ATTENTE_CONFIRMATION
                || currentRequest.getStatus() == QuoteStatus.CLIENT_ABSENT
                || currentRequest.getStatus() == QuoteStatus.TERMINE) {
            throw new IllegalStateException(
                    "Ce devis a déjà été envoyé au client. Une fois envoyé, il ne peut plus être réenvié.");
        }

        // Appliquer les mises à jour
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
    public QuoteRequest validateByClient(Long requestId, String clientIp, String deviceInfo, LocalDate requestedDeliveryDate) {
        QuoteRequest request = loadDetailedQuote(requestId);

        if (request.getStatus() != QuoteStatus.EN_ATTENTE_VALIDATION
                && request.getStatus() != QuoteStatus.DEVIS_ENVOYE) {
            throw new IllegalStateException("Ce devis ne peut pas être validé dans son état actuel.");
        }

        // Sauvegarder le PDF figé lors de la validation
        try {
            User emitter = resolveCurrentAdmin();
            if (emitter != null) {
                loadUserAddresses(emitter);
            }
            byte[] pdf = quotePdfService.generateQuote(request, emitter);
            String existingUrl = request.getQuotePdfUrl();
            
            // Sauvegarder une copie figée avec le suffixe "-validated"
            String rawBaseName = request.getQuoteNumber() != null && !request.getQuoteNumber().isBlank()
                    ? request.getQuoteNumber()
                    : request.getRequestNumber();
            String baseName = sanitizeFileName(rawBaseName);
            String validatedFilename = baseName + "-validated.pdf";
            
            String validatedPdfUrl = storageService.uploadBytes(pdf, validatedFilename, QUOTE_STORAGE_FOLDER, "application/pdf");
            request.setValidatedPdfUrl(validatedPdfUrl);
            
            // Restaurer l'URL originale si elle existait
            if (existingUrl != null && !existingUrl.isBlank()) {
                request.setQuotePdfUrl(existingUrl);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du PDF validé pour le devis {}: {}", requestId, e.getMessage(), e);
            // Ne pas bloquer la validation si le PDF ne peut pas être sauvegardé
        }

        request.setStatus(QuoteStatus.VALIDE_PAR_CLIENT);
        request.setValidatedAt(OffsetDateTime.now());
        request.setValidatedIp(clientIp);
        request.setValidatedDeviceInfo(deviceInfo);
        request.setRequestedDeliveryDate(requestedDeliveryDate);

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

        // Validation : Le devis doit être validé par le client avant l'assignation d'un livreur
        boolean isReassignmentAfterEmailFailure = request.getAssignedLivreur() != null 
                && !Boolean.TRUE.equals(request.getLivreurAssignmentEmailSent())
                && request.getStatus() == QuoteStatus.EN_COURS_LIVRAISON;
        
        if (!isReassignmentAfterEmailFailure && request.getStatus() != QuoteStatus.VALIDE_PAR_CLIENT) {
            throw new IllegalStateException("Impossible d'assigner un livreur : le devis doit être validé par le client (statut VALIDE_PAR_CLIENT) avant l'assignation.");
        }

        if (request.getAssignedLivreur() != null && Boolean.TRUE.equals(request.getLivreurAssignmentEmailSent())) {
            throw new IllegalStateException("Ce devis est déjà assigné à un livreur et l'email a été envoyé avec succès. La réassignation n'est pas autorisée.");
        }

        boolean isReassignment = request.getAssignedLivreur() != null;
        
        request.setAssignedLivreur(livreur);
        request.setDeliveryDetails(deliveryDetails != null ? deliveryDetails : request.getDeliveryDetails());
        request.setDeliveryAssignedAt(OffsetDateTime.now());
        request.setStatus(QuoteStatus.EN_COURS_LIVRAISON);
        request.setLivreurAssignmentEmailSent(false);

        QuoteRequest saved = quoteRequestRepository.save(request);
        
        boolean emailSent = mailService.notifyLivreurAssignmentSync(livreur, saved);
        if (emailSent) {
            saved.setLivreurAssignmentEmailSent(true);
            saved = quoteRequestRepository.save(saved);
            log.info("Email d'assignation envoyé avec succès pour le devis {} au livreur {}", 
                    saved.getQuoteNumber(), livreur.getEmail());
        } else {
            log.warn("Échec de l'envoi de l'email d'assignation pour le devis {} au livreur {}. La réassignation reste possible.", 
                    saved.getQuoteNumber(), livreur.getEmail());
        }
        
        return saved;
    }

    @Transactional
    public QuoteRequest markDelivered(Long requestId, User livreur, Double latitude, Double longitude, 
            String photoBase64, String signatureData, String deliveryNotes) {
        QuoteRequest request = loadDetailedQuote(requestId);

        if (request.getAssignedLivreur() == null
                || !request.getAssignedLivreur().getId().equals(livreur.getId())) {
            throw new IllegalStateException("Ce devis n'est pas assigné à ce livreur.");
        }

        if (request.getStatus() != QuoteStatus.EN_COURS_LIVRAISON 
                && request.getStatus() != QuoteStatus.CLIENT_ABSENT) {
            throw new IllegalStateException("Ce devis ne peut pas être marqué comme livré dans son état actuel.");
        }

        // Créer la preuve de livraison avec toutes les informations
        com.pneumaliback.www.entity.DeliveryProof proof = createDeliveryProof(request, livreur, latitude, longitude, photoBase64, signatureData, deliveryNotes);
        deliveryProofRepository.save(proof);

        // Mettre à jour le statut
        request.setStatus(QuoteStatus.LIVRE_EN_ATTENTE_CONFIRMATION);
        request.setDeliveryConfirmedAt(OffsetDateTime.now());

        QuoteRequest saved = quoteRequestRepository.save(request);
        mailService.notifyQuoteDelivered(saved);
        return saved;
    }

    @Transactional
    public QuoteRequest markClientAbsent(Long requestId, User livreur, String photoBase64, String notes) {
        QuoteRequest request = loadDetailedQuote(requestId);

        if (request.getAssignedLivreur() == null
                || !request.getAssignedLivreur().getId().equals(livreur.getId())) {
            throw new IllegalStateException("Ce devis n'est pas assigné à ce livreur.");
        }

        if (request.getStatus() != QuoteStatus.EN_COURS_LIVRAISON) {
            throw new IllegalStateException("Ce devis ne peut pas être marqué comme client absent dans son état actuel.");
        }

        int absentCount = (request.getClientAbsentCount() != null ? request.getClientAbsentCount() : 0) + 1;
        request.setClientAbsentCount(absentCount);

        // Créer une preuve pour l'absence si photo ou notes fournies
        if ((photoBase64 != null && !photoBase64.isBlank()) || (notes != null && !notes.isBlank())) {
            String absentNotes = "Client absent" + (notes != null && !notes.isBlank() ? " - " + notes : "");
            com.pneumaliback.www.entity.DeliveryProof proof = createDeliveryProof(request, livreur, null, null, photoBase64, null, absentNotes);
            deliveryProofRepository.save(proof);
        }

        if (absentCount >= 2) {
            // Après 2 absences, renvoyer au dépôt
            request.setStatus(QuoteStatus.EN_ATTENTE);
            request.setAssignedLivreur(null);
            request.setLivreurAssignmentEmailSent(false);
            mailService.notifyClientMultipleAbsences(request);
        } else {
            request.setStatus(QuoteStatus.CLIENT_ABSENT);
            mailService.notifyClientAbsent(request);
        }

        return quoteRequestRepository.save(request);
    }

    @Transactional
    public QuoteRequest confirmDeliveryByClient(Long requestId, User client) {
        QuoteRequest request = loadDetailedQuote(requestId);

        if (!request.getUser().getId().equals(client.getId())) {
            throw new IllegalStateException("Ce devis ne vous appartient pas.");
        }

        if (request.getStatus() != QuoteStatus.LIVRE_EN_ATTENTE_CONFIRMATION) {
            throw new IllegalStateException("Ce devis ne peut pas être confirmé dans son état actuel.");
        }

        request.setStatus(QuoteStatus.TERMINE);
        return quoteRequestRepository.save(request);
    }

    private User resolveCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmailWithAddresses(authentication.getName()).orElse(null);
    }

    /**
     * Vérifie si l'utilisateur actuellement authentifié est un développeur
     */
    private boolean isCurrentUserDeveloper() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_DEVELOPER".equals(authority));
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

    /**
     * Crée une preuve de livraison avec upload de la photo dans Supabase si fournie
     */
    private com.pneumaliback.www.entity.DeliveryProof createDeliveryProof(
            QuoteRequest request, User livreur, Double latitude, Double longitude,
            String photoBase64, String signatureData, String notes) {
        com.pneumaliback.www.entity.DeliveryProof proof = new com.pneumaliback.www.entity.DeliveryProof();
        proof.setQuoteRequest(request);
        proof.setLatitude(latitude);
        proof.setLongitude(longitude);
        proof.setSignatureData(signatureData);
        proof.setDeliveryNotes(notes);
        proof.setDeliveredByLivreur(livreur);

        // Uploader la photo dans Supabase si fournie
        if (photoBase64 != null && !photoBase64.isBlank()) {
            try {
                byte[] photoBytes = java.util.Base64.getDecoder().decode(
                    photoBase64.replaceFirst("^data:image/[^;]*;base64,", ""));
                String quoteIdentifier = request.getQuoteNumber() != null 
                    ? request.getQuoteNumber() 
                    : request.getRequestNumber();
                String photoPrefix = latitude != null ? "delivery-" : "absent-";
                String photoFilename = photoPrefix + quoteIdentifier + "-" + System.currentTimeMillis() + ".jpg";
                String photoUrl = storageService.uploadBytes(photoBytes, photoFilename, "deliveries", "image/jpeg");
                proof.setPhotoUrl(photoUrl);
                log.info("Photo uploadée dans Supabase: {}", photoUrl);
            } catch (Exception e) {
                log.error("Erreur lors de l'upload de la photo dans Supabase: {}", e.getMessage(), e);
                if (latitude != null) {
                    // Pour les livraisons, la photo est obligatoire
                    throw new IllegalStateException("Impossible de sauvegarder la photo de livraison dans Supabase", e);
                }
                // Pour les absences, on continue même si la photo échoue
            }
        }

        return proof;
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

    /**
     * Regénère tous les PDFs existants dans Supabase avec le nouveau format (sans le message du client).
     * Cette méthode remplace les anciens PDFs par les nouveaux dans Supabase.
     * 
     * @return Nombre de PDFs regénérés avec succès
     */
    @Transactional
    public int regenerateAllPdfs() {
        List<QuoteRequest> quotesWithPdf = quoteRequestRepository.findAllWithPdfUrl();
        if (quotesWithPdf.isEmpty()) {
            log.info("Aucun PDF à regénérer");
            return 0;
        }

        log.info("Début de la regénération de {} PDFs", quotesWithPdf.size());
        User defaultEmitter = resolveCurrentAdmin();
        if (defaultEmitter != null) {
            loadUserAddresses(defaultEmitter);
        }

        int successCount = 0;
        int errorCount = 0;

        for (QuoteRequest request : quotesWithPdf) {
            try {
                // Charger les données nécessaires
                QuoteRequest detailedRequest = loadDetailedQuote(request.getId());
                
                User emitter = defaultEmitter;
                if (emitter == null) {
                    // Utiliser un admin par défaut si aucun admin n'est connecté
                    emitter = userRepository.findByEmailIgnoreCase("contactlandoure@gmail.com")
                            .orElse(null);
                    if (emitter != null) {
                        loadUserAddresses(emitter);
                    }
                }

                // Regénérer le PDF avec le nouveau format
                byte[] newPdf = quotePdfService.generateQuote(detailedRequest, emitter);
                
                // Remplacer l'ancien PDF dans Supabase
                storeQuotePdf(detailedRequest, newPdf, null);
                
                // Sauvegarder la mise à jour
                quoteRequestRepository.save(detailedRequest);
                
                successCount++;
                log.debug("PDF regénéré avec succès pour le devis {}", detailedRequest.getQuoteNumber() != null 
                        ? detailedRequest.getQuoteNumber() 
                        : detailedRequest.getRequestNumber());
            } catch (Exception e) {
                errorCount++;
                log.error("Erreur lors de la regénération du PDF pour le devis {}: {}", 
                        request.getId(), e.getMessage(), e);
            }
        }

        log.info("Regénération terminée: {} succès, {} erreurs sur {} PDFs", 
                successCount, errorCount, quotesWithPdf.size());
        
        return successCount;
    }
}
