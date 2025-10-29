package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.entity.Influenceur;
import com.pneumaliback.www.dto.PromotionCreateDTO;
import com.pneumaliback.www.repository.PromotionRepository;
import com.pneumaliback.www.repository.InfluenceurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final InfluenceurService influenceurService;
    private final InfluenceurRepository influenceurRepository;

    public Optional<Promotion> findValidByCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        String normalized = code.trim();
        LocalDate today = LocalDate.now();
        return promotionRepository.findAll().stream()
                .filter(p -> normalized.equalsIgnoreCase(p.getCode()))
                .filter(p -> (p.getStartDate() == null || !today.isBefore(p.getStartDate())))
                .filter(p -> (p.getEndDate() == null || !today.isAfter(p.getEndDate())))
                .findFirst();
    }

    public Optional<Promotion> resolveFromInfluencerCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        LocalDate today = LocalDate.now();
        return influenceurService.findByPromoCode(code)
                .flatMap(inf -> promotionRepository.findAll().stream()
                        .filter(p -> p.getInfluenceur() != null && p.getInfluenceur().getId().equals(inf.getId()))
                        .filter(p -> (p.getStartDate() == null || !today.isBefore(p.getStartDate())))
                        .filter(p -> (p.getEndDate() == null || !today.isAfter(p.getEndDate())))
                        .findFirst());
    }

    public Promotion create(PromotionCreateDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Paramètres invalides");
        if (dto.code() == null || dto.code().isBlank()) throw new IllegalArgumentException("Code requis");
        if (dto.type() == null) throw new IllegalArgumentException("Type requis");
        if (dto.startDate() == null || dto.endDate() == null) throw new IllegalArgumentException("Période invalide");
        if (dto.startDate().isAfter(dto.endDate())) throw new IllegalArgumentException("La date de début doit précéder la date de fin");
        if (promotionRepository.existsByCode(dto.code().trim())) throw new IllegalArgumentException("Code promotion déjà utilisé");

        Promotion p = new Promotion();
        p.setCode(dto.code().trim());
        p.setType(dto.type());
        p.setStartDate(dto.startDate());
        p.setEndDate(dto.endDate());

        switch (dto.type()) {
            case PERCENTAGE -> {
                if (dto.discountPercentage() == null) throw new IllegalArgumentException("Pourcentage requis");
                if (dto.discountPercentage().scale() > 2 || dto.discountPercentage().signum() < 0)
                    throw new IllegalArgumentException("Pourcentage invalide");
                p.setDiscountPercentage(dto.discountPercentage());
                p.setDiscountAmount(null);
            }
            case FIXED_AMOUNT -> {
                if (dto.discountAmount() == null) throw new IllegalArgumentException("Montant requis");
                if (dto.discountAmount().scale() > 2 || dto.discountAmount().signum() <= 0)
                    throw new IllegalArgumentException("Montant invalide");
                p.setDiscountAmount(dto.discountAmount());
                p.setDiscountPercentage(null);
            }
            default -> throw new IllegalArgumentException("Type de promotion non pris en charge pour la création");
        }

        if (dto.influenceurId() != null) {
            Influenceur inf = influenceurRepository.findById(dto.influenceurId())
                    .orElseThrow(() -> new IllegalArgumentException("Influenceur introuvable"));
            p.setInfluenceur(inf);
        }

        return promotionRepository.save(p);
    }
}
