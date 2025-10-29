package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Influenceur;
import com.pneumaliback.www.repository.InfluenceurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InfluenceurService {

    private final InfluenceurRepository influenceurRepository;

    public Optional<Influenceur> findByPromoCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return influenceurRepository.findAll().stream()
                .filter(i -> code.equalsIgnoreCase(i.getPromoCode()))
                .findFirst();
    }
}
