package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

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
        if (productCount > 0) {
            throw new IllegalArgumentException("Impossible de supprimer cette catégorie car elle contient "
                    + productCount + " produit(s). Veuillez d'abord supprimer ou réaffecter les produits.");
        }

        categoryRepository.delete(category);
    }
}
