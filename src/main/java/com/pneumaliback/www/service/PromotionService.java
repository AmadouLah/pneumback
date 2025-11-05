package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.entity.Influenceur;
import com.pneumaliback.www.dto.PromotionCreateDTO;
import com.pneumaliback.www.dto.PromotionResponse;
import com.pneumaliback.www.dto.UpdatePromotionRequest;
import com.pneumaliback.www.enums.PromotionType;
import com.pneumaliback.www.repository.PromotionRepository;
import com.pneumaliback.www.repository.InfluenceurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final InfluenceurService influenceurService;
    private final InfluenceurRepository influenceurRepository;

    public Optional<Promotion> findValidByCode(String code) {
        if (code == null || code.isBlank())
            return Optional.empty();
        String normalized = code.trim();
        LocalDate today = LocalDate.now();
        return promotionRepository.findAll().stream()
                .filter(p -> normalized.equalsIgnoreCase(p.getCode()))
                .filter(p -> (p.getStartDate() == null || !today.isBefore(p.getStartDate())))
                .filter(p -> (p.getEndDate() == null || !today.isAfter(p.getEndDate())))
                .findFirst();
    }

    public Optional<Promotion> resolveFromInfluencerCode(String code) {
        if (code == null || code.isBlank())
            return Optional.empty();
        LocalDate today = LocalDate.now();
        return influenceurService.findByPromoCode(code)
                .flatMap(inf -> promotionRepository.findAll().stream()
                        .filter(p -> p.getInfluenceur() != null && p.getInfluenceur().getId().equals(inf.getId()))
                        .filter(p -> (p.getStartDate() == null || !today.isBefore(p.getStartDate())))
                        .filter(p -> (p.getEndDate() == null || !today.isAfter(p.getEndDate())))
                        .findFirst());
    }

    public PromotionResponse create(PromotionCreateDTO dto) {
        if (dto == null)
            throw new IllegalArgumentException("Paramètres invalides");
        if (dto.code() == null || dto.code().isBlank())
            throw new IllegalArgumentException("Code requis");
        if (dto.type() == null)
            throw new IllegalArgumentException("Type requis");
        if (dto.startDate() == null || dto.endDate() == null)
            throw new IllegalArgumentException("Période invalide");
        if (dto.startDate().isAfter(dto.endDate()))
            throw new IllegalArgumentException("La date de début doit précéder la date de fin");
        if (promotionRepository.existsByCode(dto.code().trim()))
            throw new IllegalArgumentException("Code promotion déjà utilisé");

        Promotion p = new Promotion();
        p.setCode(dto.code().trim());
        p.setType(dto.type());
        p.setStartDate(dto.startDate());
        p.setEndDate(dto.endDate());

        switch (dto.type()) {
            case PERCENTAGE -> {
                if (dto.discountPercentage() == null)
                    throw new IllegalArgumentException("Pourcentage requis");
                if (dto.discountPercentage().scale() > 2 || dto.discountPercentage().signum() < 0)
                    throw new IllegalArgumentException("Pourcentage invalide");
                p.setDiscountPercentage(dto.discountPercentage());
                p.setDiscountAmount(null);
            }
            case FIXED_AMOUNT -> {
                if (dto.discountAmount() == null)
                    throw new IllegalArgumentException("Montant requis");
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

        return toResponse(promotionRepository.save(p));
    }

    public List<PromotionResponse> findAll() {
        return promotionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<PromotionResponse> findById(Long id) {
        return promotionRepository.findById(id)
                .map(this::toResponse);
    }

    @Transactional
    public PromotionResponse update(Long id, UpdatePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion introuvable"));

        if (request.code() != null && !request.code().isBlank()) {
            String code = request.code().trim();
            if (promotionRepository.existsByCodeAndIdNot(code, id)) {
                throw new IllegalArgumentException("Code promotion déjà utilisé");
            }
            promotion.setCode(code);
        }

        if (request.type() != null) {
            promotion.setType(request.type());
        }

        if (request.startDate() != null) {
            promotion.setStartDate(request.startDate());
        }

        if (request.endDate() != null) {
            promotion.setEndDate(request.endDate());
        }

        if (request.startDate() != null && request.endDate() != null) {
            if (request.startDate().isAfter(request.endDate())) {
                throw new IllegalArgumentException("La date de début doit précéder la date de fin");
            }
        } else if (request.startDate() != null && promotion.getEndDate() != null) {
            if (request.startDate().isAfter(promotion.getEndDate())) {
                throw new IllegalArgumentException("La date de début doit précéder la date de fin");
            }
        } else if (request.endDate() != null && promotion.getStartDate() != null) {
            if (promotion.getStartDate().isAfter(request.endDate())) {
                throw new IllegalArgumentException("La date de début doit précéder la date de fin");
            }
        }

        PromotionType type = request.type() != null ? request.type() : promotion.getType();
        switch (type) {
            case PERCENTAGE -> {
                if (request.discountPercentage() != null) {
                    if (request.discountPercentage().scale() > 2 || request.discountPercentage().signum() < 0) {
                        throw new IllegalArgumentException("Pourcentage invalide");
                    }
                    promotion.setDiscountPercentage(request.discountPercentage());
                    promotion.setDiscountAmount(null);
                }
            }
            case FIXED_AMOUNT -> {
                if (request.discountAmount() != null) {
                    if (request.discountAmount().scale() > 2 || request.discountAmount().signum() <= 0) {
                        throw new IllegalArgumentException("Montant invalide");
                    }
                    promotion.setDiscountAmount(request.discountAmount());
                    promotion.setDiscountPercentage(null);
                }
            }
        }

        if (request.influenceurId() != null) {
            Influenceur influenceur = influenceurRepository.findById(request.influenceurId())
                    .orElseThrow(() -> new IllegalArgumentException("Influenceur introuvable"));
            promotion.setInfluenceur(influenceur);
        }

        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional
    public void delete(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion introuvable"));
        promotionRepository.delete(promotion);
    }

    private PromotionResponse toResponse(Promotion promotion) {
        PromotionResponse.InfluenceurInfo influenceurInfo = null;
        if (promotion.getInfluenceur() != null && promotion.getInfluenceur().getUser() != null) {
            var user = promotion.getInfluenceur().getUser();
            influenceurInfo = new PromotionResponse.InfluenceurInfo(
                    promotion.getInfluenceur().getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail());
        }
        return new PromotionResponse(
                promotion.getId(),
                promotion.getCode(),
                promotion.getType(),
                promotion.getDiscountPercentage(),
                promotion.getDiscountAmount(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                influenceurInfo);
    }
}
