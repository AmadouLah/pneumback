package com.pneumaliback.www.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.QuoteRequestItem;
import com.pneumaliback.www.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuotePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final CarboneClient carboneClient;

    public byte[] generateQuote(QuoteRequest request, User emitter) {
        Map<String, Object> templateData = buildTemplateData(request, emitter);
        return carboneClient.renderPdf(templateData);
    }

    private Map<String, Object> buildTemplateData(QuoteRequest request, User emitter) {
        Map<String, Object> root = new LinkedHashMap<>();

        root.put("quote", buildQuoteMeta(request));
        root.put("emitter", buildEmitter(emitter));
        root.put("client", buildClient(request));

        List<Map<String, Object>> items = request.getItems().stream()
                .map(this::mapItem)
                .toList();
        root.put("items", items);
        root.put("totals", buildTotals(request));
        root.put("notes", buildNotes(request));

        return root;
    }

    private Map<String, Object> buildQuoteMeta(QuoteRequest request) {
        Map<String, Object> quoteMeta = new LinkedHashMap<>();
        quoteMeta.put("requestNumber", request.getRequestNumber());
        quoteMeta.put("quoteNumber", request.getQuoteNumber() != null ? request.getQuoteNumber() : "À confirmer");
        quoteMeta.put("status", request.getStatus().name());
        quoteMeta.put("createdAt", DATE_FORMAT.format(request.getCreatedAt().toLocalDate()));
        quoteMeta.put("validUntil",
                request.getValidUntil() != null ? DATE_FORMAT.format(request.getValidUntil()) : null);
        return quoteMeta;
    }

    private Map<String, Object> buildEmitter(User emitter) {
        Map<String, Object> map = new LinkedHashMap<>();
        String defaultAddress = "Bamako, Mali";
        if (emitter == null) {
            map.put("name", "Landouré Amadou");
            map.put("email", "amadoulandoure004@gmail.com");
            map.put("phone", "+223 70 91 11 12");
            map.put("address", defaultAddress);
            return map;
        }
        map.put("name", safe(emitter.getFullName()));
        map.put("email", safe(emitter.getEmail()));
        map.put("phone", safe(emitter.getPhoneNumber()));
        map.put("address", formatAddress(resolvePreferredAddress(emitter), defaultAddress));
        return map;
    }

    private Map<String, Object> buildClient(QuoteRequest request) {
        Map<String, Object> client = new LinkedHashMap<>();
        User user = request.getUser();
        client.put("name", user.getFullName());
        client.put("email", user.getEmail());
        client.put("phone", safe(user.getPhoneNumber()));
        client.put("requestNumber", request.getRequestNumber());
        client.put("address", formatAddress(resolvePreferredAddress(user), "Non communiqué"));
        return client;
    }

    private Map<String, Object> buildTotals(QuoteRequest request) {
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("subtotal", formatCurrency(request.getSubtotalRequested()));
        totals.put("discount", formatCurrency(request.getDiscountTotal()));
        totals.put("total",
                formatCurrency(request.getTotalQuoted() != null ? request.getTotalQuoted()
                        : request.getSubtotalRequested()));
        return totals;
    }

    private Map<String, Object> buildNotes(QuoteRequest request) {
        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("clientMessage", request.getClientMessage());
        notes.put("adminNotes", request.getAdminNotes());
        notes.put("delivery", request.getDeliveryDetails());
        return notes;
    }

    private Map<String, Object> mapItem(QuoteRequestItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", item.getProductName());
        row.put("description", buildDescription(item));
        row.put("brand", item.getBrandName());
        row.put("width", item.getWidthValue());
        row.put("profile", item.getProfileValue());
        row.put("diameter", item.getDiameterValue());
        row.put("quantity", item.getQuantity());
        row.put("unitPrice", formatCurrency(item.getUnitPrice()));
        row.put("total", formatCurrency(item.getLineTotal()));
        return row;
    }

    private String buildDescription(QuoteRequestItem item) {
        StringBuilder description = new StringBuilder();
        description.append(item.getProductName());
        if (item.getBrandName() != null) {
            description.append(" • ").append(item.getBrandName());
        }
        if (item.getWidthValue() != null && item.getProfileValue() != null && item.getDiameterValue() != null) {
            description.append(" — ")
                    .append(item.getWidthValue())
                    .append("/")
                    .append(item.getProfileValue())
                    .append(" R")
                    .append(item.getDiameterValue());
        }
        return description.toString();
    }

    private Address resolvePreferredAddress(User user) {
        if (user == null || user.getAddresses() == null || user.getAddresses().isEmpty()) {
            return null;
        }
        return user.getAddresses().stream()
                .filter(Objects::nonNull)
                .filter(Address::isDefault)
                .findFirst()
                .orElse(user.getAddresses().stream().filter(Objects::nonNull).findFirst().orElse(null));
    }

    private String formatAddress(Address address, String fallback) {
        if (address == null) {
            return fallback;
        }
        StringBuilder formatted = new StringBuilder();
        if (address.getStreet() != null && !address.getStreet().isBlank()) {
            formatted.append(address.getStreet());
        }
        if (address.getCity() != null && !address.getCity().isBlank()) {
            appendPart(formatted, address.getCity());
        }
        if (address.getRegion() != null && !address.getRegion().isBlank()) {
            appendPart(formatted, address.getRegion());
        }
        if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
            appendPart(formatted, address.getPostalCode());
        }
        appendPart(formatted, address.getCountry() != null ? address.getCountry().getDisplayName() : null);
        String built = formatted.toString().trim();
        return built.isBlank() ? fallback : built;
    }

    private void appendPart(StringBuilder builder, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(part.trim());
    }

    private String safe(String value) {
        return value != null ? value : "Non communiqué";
    }

    private String formatCurrency(BigDecimal value) {
        BigDecimal safeValue = value != null ? value : BigDecimal.ZERO;
        return String.format(Locale.FRENCH, "%,.2f FCFA", safeValue);
    }
}
