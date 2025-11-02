package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.CreateProductRequest;
import com.pneumaliback.www.dto.UpdateProductRequest;
import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.repository.CategoryRepository;
import com.pneumaliback.www.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Page<Product> listActive(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable);
    }

    public Page<Product> findWithFilters(Category category,
            String brand,
            String size,
            String season,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return productRepository.findWithFilters(category, brand, size, season, minPrice, maxPrice, pageable);
    }

    public Page<Product> searchActive(String searchTerm, Pageable pageable) {
        return productRepository.searchProducts(searchTerm, pageable);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public Page<Product> popular(Pageable pageable) {
        return productRepository.findPopular(pageable);
    }

    public List<String> brands() {
        return productRepository.findAllDistinctBrands();
    }

    public Page<Product> findByDimensions(String width, String profile, String diameter, Pageable pageable) {
        return productRepository.findByDimensions(width, profile, diameter, pageable);
    }

    public Page<Product> listAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produit introuvable"));
    }

    @Transactional
    public Product createFromRequest(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));

        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock() != null ? request.stock() : 0);
        product.setBrand(request.brand());
        product.setSize(request.size());
        product.setSeason(request.season());
        product.setVehicleType(request.vehicleType());
        product.setImageUrl(request.imageUrl());
        product.setDescription(request.description());
        product.setCategory(category);
        product.setActive(request.active() != null ? request.active() : true);

        return productRepository.save(product);
    }

    @Transactional
    public Product updateFromRequest(Long id, UpdateProductRequest request) {
        Product product = findById(id);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.stock() != null) {
            product.setStock(request.stock());
        }
        if (request.brand() != null) {
            product.setBrand(request.brand());
        }
        if (request.size() != null) {
            product.setSize(request.size());
        }
        if (request.season() != null) {
            product.setSeason(request.season());
        }
        if (request.vehicleType() != null) {
            product.setVehicleType(request.vehicleType());
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));
            product.setCategory(category);
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }

        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
    }
}
