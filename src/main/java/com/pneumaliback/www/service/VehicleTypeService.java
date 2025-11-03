package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.entity.VehicleType;
import com.pneumaliback.www.repository.CategoryRepository;
import com.pneumaliback.www.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final CategoryRepository categoryRepository;

    public List<VehicleType> listAll() {
        return vehicleTypeRepository.findAll();
    }

    public List<VehicleType> listActive() {
        return vehicleTypeRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<VehicleType> listByCategory(Long categoryId) {
        return vehicleTypeRepository.findByCategoryIdAndActiveTrueOrderByNameAsc(categoryId);
    }

    public VehicleType toggleActive(Long id, boolean active) {
        VehicleType v = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de véhicule introuvable"));
        v.setActive(active);
        return vehicleTypeRepository.save(v);
    }

    public VehicleType create(VehicleType payload) {
        Category category = categoryRepository.findById(payload.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));

        // Vérifier si le nom existe déjà pour cette catégorie
        vehicleTypeRepository.findByNameIgnoreCaseAndCategory(payload.getName(), category)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Un type de véhicule avec ce nom existe déjà pour cette catégorie");
                });

        VehicleType vehicleType = new VehicleType();
        vehicleType.setName(payload.getName());
        vehicleType.setDescription(payload.getDescription());
        vehicleType.setCategory(category);
        vehicleType.setActive(payload.isActive());

        return vehicleTypeRepository.save(vehicleType);
    }

    public VehicleType update(Long id, VehicleType payload) {
        VehicleType v = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de véhicule introuvable"));

        // Si la catégorie change, vérifier qu'elle existe
        if (payload.getCategory() != null && payload.getCategory().getId() != null) {
            Category category = categoryRepository.findById(payload.getCategory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));
            v.setCategory(category);
        }

        // Vérifier si le nom existe déjà pour cette catégorie (sauf pour l'entité
        // actuelle)
        if (payload.getName() != null && !payload.getName().equals(v.getName())) {
            Category categoryToCheck = v.getCategory();
            if (payload.getCategory() != null && payload.getCategory().getId() != null) {
                categoryToCheck = categoryRepository.findById(payload.getCategory().getId())
                        .orElse(v.getCategory());
            }
            vehicleTypeRepository.findByNameIgnoreCaseAndCategory(payload.getName(), categoryToCheck)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            throw new IllegalArgumentException(
                                    "Un type de véhicule avec ce nom existe déjà pour cette catégorie");
                        }
                    });
        }

        if (payload.getName() != null) {
            v.setName(payload.getName());
        }
        if (payload.getDescription() != null) {
            v.setDescription(payload.getDescription());
        }
        v.setActive(payload.isActive());

        return vehicleTypeRepository.save(v);
    }

    @Transactional
    public void delete(Long id) {
        VehicleType vehicleType = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Type de véhicule introuvable"));

        long productCount = vehicleTypeRepository.countProductsByVehicleTypeId(id);
        if (productCount > 0) {
            throw new IllegalArgumentException("Impossible de supprimer ce type de véhicule car il est utilisé par "
                    + productCount + " produit(s). Veuillez d'abord supprimer ou réaffecter les produits.");
        }

        vehicleTypeRepository.delete(vehicleType);
    }
}
