package com.pneumaliback.www.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.pneumaliback.www.enums.QuoteStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "quote_requests", indexes = {
        @Index(name = "idx_quote_requests_number", columnList = "request_number", unique = true),
        @Index(name = "idx_quote_requests_quote_number", columnList = "quote_number", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
public class QuoteRequest extends EntiteAuditable {

    @Column(name = "request_number", nullable = false, length = 32, unique = true)
    private String requestNumber;

    @Column(name = "quote_number", length = 32, unique = true)
    private String quoteNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuoteStatus status = QuoteStatus.EN_ATTENTE;

    @Column(name = "client_message", columnDefinition = "TEXT")
    private String clientMessage;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "subtotal_requested", precision = 12, scale = 2)
    private BigDecimal subtotalRequested;

    @Column(name = "discount_total", precision = 12, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "total_quoted", precision = 12, scale = 2)
    private BigDecimal totalQuoted;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "quote_pdf_url")
    private String quotePdfUrl;

    @Column(name = "validated_at")
    private OffsetDateTime validatedAt;

    @Column(name = "validated_ip", length = 64)
    private String validatedIp;

    @Column(name = "validated_device_info", length = 256)
    private String validatedDeviceInfo;

    @Column(name = "validated_pdf_url")
    private String validatedPdfUrl;

    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;

    @Column(name = "client_absent_count", nullable = false)
    private Integer clientAbsentCount = 0;

    @Column(name = "delivery_assigned_at")
    private OffsetDateTime deliveryAssignedAt;

    @Column(name = "delivery_confirmed_at")
    private OffsetDateTime deliveryConfirmedAt;

    @ManyToOne
    @JoinColumn(name = "assigned_livreur_id")
    private User assignedLivreur;

    @Column(name = "livreur_assignment_email_sent", nullable = false)
    private Boolean livreurAssignmentEmailSent = false;

    @Column(name = "delivery_details", columnDefinition = "TEXT")
    private String deliveryDetails;

    @OneToMany(mappedBy = "quoteRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuoteRequestItem> items = new ArrayList<>();
}
