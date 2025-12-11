package com.pneumaliback.www.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "delivery_proofs")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeliveryProof extends EntiteAuditable {

    @OneToOne
    @JoinColumn(name = "quote_request_id", nullable = false, unique = true)
    private QuoteRequest quoteRequest;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "photo_url")
    private String photoUrl;

    @Lob
    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    @ManyToOne
    @JoinColumn(name = "delivered_by_livreur_id", nullable = false)
    private User deliveredByLivreur;
}

