package com.pneumaliback.www.service;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pneumaliback.www.entity.NumberSequence;
import com.pneumaliback.www.repository.NumberSequenceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NumberSequenceService {

    private final NumberSequenceRepository repository;

    @Transactional
    public long nextValue(String sequenceKey) {
        String normalizedKey = normalizeKey(sequenceKey);
        int year = LocalDate.now().getYear();

        NumberSequence sequence = repository.findBySequenceKeyAndYear(normalizedKey, year)
                .orElseGet(() -> {
                    NumberSequence created = new NumberSequence();
                    created.setSequenceKey(normalizedKey);
                    created.setYear(year);
                    created.setLastValue(0L);
                    return repository.save(created);
                });

        long next = sequence.getLastValue() + 1;
        sequence.setLastValue(next);
        repository.save(sequence);
        return next;
    }

    @Transactional
    public String nextFormatted(String sequenceKey, String prefix) {
        int year = LocalDate.now().getYear();
        long value = nextValue(sequenceKey);
        return String.format(Locale.FRENCH, "%s-%d-%04d", prefix, year, value);
    }

    private String normalizeKey(String key) {
        return key == null ? "DEFAULT" : key.trim().toUpperCase(Locale.ROOT);
    }
}
