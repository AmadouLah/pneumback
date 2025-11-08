package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.repository.CategoryRepository;
import com.pneumaliback.www.repository.VehicleTypeRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    public List<Category> listActive() {
        return categoryRepository.findByActiveTrue();
    }

    public Category toggleActive(Long id, boolean active) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        c.setActive(active);
        return categoryRepository.save(c);
    }

    public Category create(Category c) {
        return categoryRepository.save(c);
    }

    public Category update(Long id, Category payload) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        c.setName(payload.getName());
        c.setDescription(payload.getDescription());
        c.setActive(payload.isActive());
        return categoryRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));

        long productCount = categoryRepository.countAllProductsInCategory(id);
        long vehicleTypeCount = vehicleTypeRepository.countByCategoryId(id);

        if (productCount > 0 || vehicleTypeCount > 0) {
            throw new IllegalArgumentException(buildBlockingMessage(productCount, vehicleTypeCount));
        }

        categoryRepository.delete(category);
    }

    private String buildBlockingMessage(long productCount, long vehicleTypeCount) {
        List<String> reasons = new ArrayList<>();
        if (productCount > 0) {
            reasons.add(productCount + (productCount > 1 ? " produits" : " produit"));
        }
        if (vehicleTypeCount > 0) {
            reasons.add(vehicleTypeCount + (vehicleTypeCount > 1 ? " types de véhicule" : " type de véhicule"));
        }

        String detail = String.join(" et ", reasons);
        return "Impossible de supprimer cette catégorie car elle est liée à " + detail
                + ". Veuillez d'abord réaffecter ou supprimer ces éléments.";
    }
}
