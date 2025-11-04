package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.TireCondition;
import com.pneumaliback.www.repository.TireConditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TireConditionService {

    private final TireConditionRepository tireConditionRepository;

    public List<TireCondition> listAll() {
        return tireConditionRepository.findAll();
    }

    public List<TireCondition> listActive() {
        return tireConditionRepository.findByActiveTrueOrderByNameAsc();
    }

    public TireCondition toggleActive(Long id, boolean active) {
        TireCondition t = tireConditionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("État de pneu introuvable"));
        t.setActive(active);
        return tireConditionRepository.save(t);
    }

    public TireCondition create(TireCondition payload) {
        tireConditionRepository.findByNameIgnoreCase(payload.getName())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Un état de pneu avec ce nom existe déjà");
                });

        TireCondition tireCondition = new TireCondition();
        tireCondition.setName(payload.getName());
        tireCondition.setDescription(payload.getDescription());
        tireCondition.setActive(payload.isActive());

        return tireConditionRepository.save(tireCondition);
    }

    public TireCondition update(Long id, TireCondition payload) {
        TireCondition t = tireConditionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("État de pneu introuvable"));

        if (payload.getName() != null && !payload.getName().equals(t.getName())) {
            tireConditionRepository.findByNameIgnoreCase(payload.getName())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            throw new IllegalArgumentException("Un état de pneu avec ce nom existe déjà");
                        }
                    });
            t.setName(payload.getName());
        }
        if (payload.getDescription() != null) {
            t.setDescription(payload.getDescription());
        }
        t.setActive(payload.isActive());

        return tireConditionRepository.save(t);
    }

    @Transactional
    public void delete(Long id) {
        TireCondition tireCondition = tireConditionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("État de pneu introuvable"));

        long productCount = tireConditionRepository.countProductsByTireConditionId(id);
        if (productCount > 0) {
            throw new IllegalArgumentException("Impossible de supprimer cet état de pneu car il est utilisé par "
                    + productCount + " produit(s). Veuillez d'abord supprimer ou réaffecter les produits.");
        }

        tireConditionRepository.delete(tireCondition);
    }
}
