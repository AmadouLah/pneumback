package com.pneumaliback.www.dto.quote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.QuoteRequestItem;
import com.pneumaliback.www.enums.QuoteStatus;

public record QuoteResponse(
        Long id,
        String requestNumber,
        String quoteNumber,
        QuoteStatus status,
        BigDecimal subtotalRequested,
        BigDecimal discountTotal,
        BigDecimal totalQuoted,
        LocalDate validUntil,
        String quotePdfUrl,
        OffsetDateTime validatedAt,
        String validatedIp,
        String validatedDeviceInfo,
        String validatedPdfUrl,
        LocalDate requestedDeliveryDate,
        Integer clientAbsentCount,
        String clientEmail,
        String clientName,
        String clientMessage,
        String adminNotes,
        String deliveryDetails,
        String assignedLivreur,
        Boolean livreurAssignmentEmailSent,
        LocalDateTime updatedAt,
        List<QuoteItemResponse> items) {

    public static QuoteResponse from(QuoteRequest request) {
        return new QuoteResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getQuoteNumber(),
                request.getStatus(),
                request.getSubtotalRequested(),
                request.getDiscountTotal(),
                request.getTotalQuoted(),
                request.getValidUntil(),
                request.getQuotePdfUrl(),
                request.getValidatedAt(),
                request.getValidatedIp(),
                request.getValidatedDeviceInfo(),
                request.getValidatedPdfUrl(),
                request.getRequestedDeliveryDate(),
                request.getClientAbsentCount(),
                request.getUser().getEmail(),
                buildClientName(request),
                request.getClientMessage(),
                request.getAdminNotes(),
                request.getDeliveryDetails(),
                request.getAssignedLivreur() != null ? request.getAssignedLivreur().getEmail() : null,
                request.getLivreurAssignmentEmailSent(),
                request.getUpdatedAt(),
                request.getItems().stream()
                        .map(QuoteResponse::mapItem)
                        .toList());
    }

    private static QuoteItemResponse mapItem(QuoteRequestItem item) {
        return new QuoteItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getBrandName(),
                item.getWidthValue(),
                item.getProfileValue(),
                item.getDiameterValue(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }

    private static String buildClientName(QuoteRequest request) {
        String firstName = request.getUser().getFirstName();
        String lastName = request.getUser().getLastName();
        String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        return fullName.isEmpty() ? request.getUser().getEmail() : fullName;
    }
}
