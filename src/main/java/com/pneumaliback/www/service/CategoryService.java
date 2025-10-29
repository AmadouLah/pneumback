package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> listActive() { return categoryRepository.findByActiveTrue(); }

    public Category toggleActive(Long id, boolean active) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        c.setActive(active);
        return categoryRepository.save(c);
    }

    public Category create(Category c) { return categoryRepository.save(c); }

    public Category update(Long id, Category payload) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
        c.setName(payload.getName());
        c.setDescription(payload.getDescription());
        c.setActive(payload.isActive());
        return categoryRepository.save(c);
    }
}
