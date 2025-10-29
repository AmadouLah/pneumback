package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

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
}

