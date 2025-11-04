package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.CreateProductRequest;
import com.pneumaliback.www.dto.UpdateProductRequest;
import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.entity.Brand;
import com.pneumaliback.www.entity.TireWidth;
import com.pneumaliback.www.entity.TireProfile;
import com.pneumaliback.www.entity.TireDiameter;
import com.pneumaliback.www.entity.VehicleType;
import com.pneumaliback.www.entity.TireCondition;
import com.pneumaliback.www.repository.CategoryRepository;
import com.pneumaliback.www.repository.ProductRepository;
import com.pneumaliback.www.repository.BrandRepository;
import com.pneumaliback.www.repository.TireWidthRepository;
import com.pneumaliback.www.repository.TireProfileRepository;
import com.pneumaliback.www.repository.TireDiameterRepository;
import com.pneumaliback.www.repository.VehicleTypeRepository;
import com.pneumaliback.www.repository.TireConditionRepository;
import com.pneumaliback.www.repository.CartItemRepository;
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
    private final BrandRepository brandRepository;
    private final TireWidthRepository tireWidthRepository;
    private final TireProfileRepository tireProfileRepository;
    private final TireDiameterRepository tireDiameterRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final TireConditionRepository tireConditionRepository;
    private final CartItemRepository cartItemRepository;

    public Page<Product> listActive(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable);
    }

    public Page<Product> findWithFilters(Category category,
            Brand brand,
            String size,
            String season,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return productRepository.findWithFilters(category, brand, size, season, minPrice, maxPrice, pageable);
    }

    public List<Brand> brands() {
        return productRepository.findAllActiveBrands();
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

    public Page<Product> findByDimensions(String width, String profile, String diameter, Pageable pageable) {
        // Convertir les strings en integers si non null
        Integer widthInt = null;
        Integer profileInt = null;
        Integer diameterInt = null;

        try {
            if (width != null && !width.isBlank()) {
                widthInt = Integer.parseInt(width);
            }
        } catch (NumberFormatException e) {
            // Ignorer si ce n'est pas un nombre valide
        }

        try {
            if (profile != null && !profile.isBlank()) {
                profileInt = Integer.parseInt(profile);
            }
        } catch (NumberFormatException e) {
            // Ignorer si ce n'est pas un nombre valide
        }

        try {
            if (diameter != null && !diameter.isBlank()) {
                diameterInt = Integer.parseInt(diameter);
            }
        } catch (NumberFormatException e) {
            // Ignorer si ce n'est pas un nombre valide
        }

        return productRepository.findByDimensions(width, profile, diameter, widthInt, profileInt, diameterInt,
                pageable);
    }

    public Page<Product> listAll(Pageable pageable) {
        return productRepository.findAllWithDimensions(pageable);
    }

    public Product findById(Long id) {
        return productRepository.findByIdWithDimensions(id)
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
        product.setSize(request.size());

        if (request.brandId() != null) {
            Brand brand = brandRepository.findById(request.brandId())
                    .orElseThrow(() -> new IllegalArgumentException("Marque introuvable"));
            product.setBrand(brand);
        }

        if (request.widthId() != null) {
            TireWidth width = tireWidthRepository.findById(request.widthId())
                    .orElseThrow(() -> new IllegalArgumentException("Largeur introuvable"));
            product.setWidth(width);
        }

        if (request.profileId() != null) {
            TireProfile profile = tireProfileRepository.findById(request.profileId())
                    .orElseThrow(() -> new IllegalArgumentException("Profil introuvable"));
            product.setProfile(profile);
        }

        if (request.diameterId() != null) {
            TireDiameter diameter = tireDiameterRepository.findById(request.diameterId())
                    .orElseThrow(() -> new IllegalArgumentException("Diamètre introuvable"));
            product.setDiameter(diameter);
        }

        if (request.vehicleTypeId() != null) {
            VehicleType vehicleType = vehicleTypeRepository.findById(request.vehicleTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Type de véhicule introuvable"));
            product.setVehicleType(vehicleType);
        }

        if (request.tireConditionId() != null) {
            TireCondition tireCondition = tireConditionRepository.findById(request.tireConditionId())
                    .orElseThrow(() -> new IllegalArgumentException("État de pneu introuvable"));
            product.setTireCondition(tireCondition);
        }

        product.setSeason(request.season());
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

        if (request.brandId() != null) {
            Brand brand = brandRepository.findById(request.brandId())
                    .orElseThrow(() -> new IllegalArgumentException("Marque introuvable"));
            product.setBrand(brand);
        }

        if (request.size() != null) {
            product.setSize(request.size());
        }

        if (request.widthId() != null) {
            TireWidth width = tireWidthRepository.findById(request.widthId())
                    .orElseThrow(() -> new IllegalArgumentException("Largeur introuvable"));
            product.setWidth(width);
        }

        if (request.profileId() != null) {
            TireProfile profile = tireProfileRepository.findById(request.profileId())
                    .orElseThrow(() -> new IllegalArgumentException("Profil introuvable"));
            product.setProfile(profile);
        }

        if (request.diameterId() != null) {
            TireDiameter diameter = tireDiameterRepository.findById(request.diameterId())
                    .orElseThrow(() -> new IllegalArgumentException("Diamètre introuvable"));
            product.setDiameter(diameter);
        }

        if (request.season() != null) {
            product.setSeason(request.season());
        }
        if (request.vehicleTypeId() != null) {
            VehicleType vehicleType = vehicleTypeRepository.findById(request.vehicleTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Type de véhicule introuvable"));
            product.setVehicleType(vehicleType);
        }
        if (request.tireConditionId() != null) {
            TireCondition tireCondition = tireConditionRepository.findById(request.tireConditionId())
                    .orElseThrow(() -> new IllegalArgumentException("État de pneu introuvable"));
            product.setTireCondition(tireCondition);
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
        // Supprimer tous les CartItem qui référencent ce produit
        cartItemRepository.deleteByProductId(id);
        productRepository.delete(product);
    }
}
