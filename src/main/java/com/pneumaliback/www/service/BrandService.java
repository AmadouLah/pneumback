package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Brand;
import com.pneumaliback.www.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<Brand> listAll() {
        return brandRepository.findAll();
    }

    public List<Brand> listActive() {
        return brandRepository.findByActiveTrue();
    }

    public Brand toggleActive(Long id, boolean active) {
        Brand b = brandRepository.findById(id).orElseThrow(() -> new RuntimeException("Marque introuvable"));
        b.setActive(active);
        return brandRepository.save(b);
    }

    public Brand create(Brand b) {
        if (b.getName() == null || b.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la marque est requis");
        }
        if (brandRepository.existsByNameIgnoreCase(b.getName().trim())) {
            throw new IllegalArgumentException("Une marque avec ce nom existe déjà");
        }
        b.setName(b.getName().trim());
        return brandRepository.save(b);
    }

    public Brand update(Long id, Brand payload) {
        Brand b = brandRepository.findById(id).orElseThrow(() -> new RuntimeException("Marque introuvable"));
        if (payload.getName() == null || payload.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la marque est requis");
        }
        // Vérifier si le nouveau nom existe déjà pour une autre marque
        Optional<Brand> existing = brandRepository.findByNameIgnoreCase(payload.getName().trim());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Une marque avec ce nom existe déjà");
        }
        b.setName(payload.getName().trim());
        b.setActive(payload.isActive());
        return brandRepository.save(b);
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marque introuvable"));

        long productCount = brandRepository.countAllProductsInBrand(id);
        if (productCount > 0) {
            throw new IllegalArgumentException("Impossible de supprimer cette marque car elle contient "
                    + productCount + " produit(s). Veuillez d'abord supprimer ou réaffecter les produits.");
        }

        brandRepository.delete(brand);
    }
}
