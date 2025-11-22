package com.pneumaliback.www.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "number_sequences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_number_sequence_key_year", columnNames = { "sequence_key", "year" })
})
@Data
@EqualsAndHashCode(callSuper = true)
public class NumberSequence extends EntiteAuditable {

    @Column(name = "sequence_key", nullable = false, length = 64)
    private String sequenceKey;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "last_value", nullable = false)
    private long lastValue;
}
