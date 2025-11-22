package com.pneumaliback.www.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "quote_request_items")
@Data
@EqualsAndHashCode(callSuper = true)
public class QuoteRequestItem extends EntiteAuditable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_request_id")
    private QuoteRequest quoteRequest;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "width_value")
    private Integer widthValue;

    @Column(name = "profile_value")
    private Integer profileValue;

    @Column(name = "diameter_value")
    private Integer diameterValue;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 12, scale = 2)
    private BigDecimal lineTotal;
}
