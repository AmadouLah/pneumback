package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.CreateProductRequest;
import com.pneumaliback.www.dto.UpdateProductRequest;
import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.repository.CategoryRepository;
import com.pneumaliback.www.service.ProductService;
import com.pneumaliback.www.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.IOException;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Produits", description = "Consultation et gestion des produits")
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final StorageService storageService;

    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            String msg = e.getMessage() != null ? e.getMessage() : "Requête invalide";
            if (msg.toLowerCase().contains("introuvable") || msg.toLowerCase().contains("non trouv")) {
                return ResponseEntity.status(404).body(java.util.Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", msg));
        }
        return ResponseEntity.internalServerError()
                .body(java.util.Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
    }

    @GetMapping("/active")
    @Operation(summary = "Lister produits actifs (page)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listActive(Pageable pageable) {
        try {
            return ResponseEntity.ok(productService.listActive(pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher dans les produits actifs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Résultats récupérés"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> search(@RequestParam String term, Pageable pageable) {
        try {
            return ResponseEntity.ok(productService.searchActive(term, pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/filter")
    @Operation(summary = "Filtrer les produits actifs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Résultats filtrés"),
            @ApiResponse(responseCode = "404", description = "Catégorie introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> filter(@RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        try {
            Category category = null;
            if (categoryId != null) {
                category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));
            }
            return ResponseEntity
                    .ok(productService.findWithFilters(category, brand, size, season, minPrice, maxPrice, pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping
    @Operation(summary = "Lister tous les produits (admin)", description = "Liste tous les produits, y compris inactifs")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée"),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listAll(Pageable pageable) {
        try {
            return ResponseEntity.ok(productService.listAll(pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un produit par ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit trouvé"),
            @ApiResponse(responseCode = "404", description = "Produit introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.findById(id));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping
    @Operation(summary = "Créer un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit créé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> create(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("name") String name,
            @RequestParam("price") String priceStr,
            @RequestParam("stock") String stockStr,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "size", required = false) String size,
            @RequestParam(value = "season", required = false) String season,
            @RequestParam(value = "vehicleType", required = false) String vehicleType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("categoryId") String categoryIdStr,
            @RequestParam(value = "active", defaultValue = "true") String activeStr) {
        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                imageUrl = storageService.uploadFile(image, "products");
            }

            BigDecimal price = new BigDecimal(priceStr);
            Integer stock = Integer.parseInt(stockStr);
            Long categoryId = Long.parseLong(categoryIdStr);
            Boolean active = "true".equalsIgnoreCase(activeStr);

            CreateProductRequest request = new CreateProductRequest(
                    name, price, stock, brand, size,
                    season != null && !season.isBlank() ? com.pneumaliback.www.enums.TireSeason.valueOf(season) : null,
                    vehicleType != null && !vehicleType.isBlank()
                            ? com.pneumaliback.www.enums.VehicleType.valueOf(vehicleType)
                            : null,
                    imageUrl, description, categoryId, active);

            Product product = productService.createFromRequest(request);
            return ResponseEntity.ok(product);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Format numérique invalide"));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Erreur lors de l'upload de l'image: " + e.getMessage()));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "404", description = "Produit introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "price", required = false) String priceStr,
            @RequestParam(value = "stock", required = false) String stockStr,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "size", required = false) String size,
            @RequestParam(value = "season", required = false) String season,
            @RequestParam(value = "vehicleType", required = false) String vehicleType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) String categoryIdStr,
            @RequestParam(value = "active", required = false) String activeStr) {
        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                Product existingProduct = productService.findById(id);
                if (existingProduct.getImageUrl() != null && !existingProduct.getImageUrl().isBlank()) {
                    String oldFilePath = storageService.extractFilePathFromUrl(existingProduct.getImageUrl());
                    if (oldFilePath != null) {
                        storageService.deleteFile(oldFilePath);
                    }
                }
                imageUrl = storageService.uploadFile(image, "products");
            }

            BigDecimal price = priceStr != null && !priceStr.isBlank() ? new BigDecimal(priceStr) : null;
            Integer stock = stockStr != null && !stockStr.isBlank() ? Integer.parseInt(stockStr) : null;
            Long categoryId = categoryIdStr != null && !categoryIdStr.isBlank() ? Long.parseLong(categoryIdStr) : null;
            Boolean active = activeStr != null && !activeStr.isBlank() ? "true".equalsIgnoreCase(activeStr) : null;

            UpdateProductRequest request = new UpdateProductRequest(
                    name, price, stock, brand, size,
                    season != null && !season.isBlank() ? com.pneumaliback.www.enums.TireSeason.valueOf(season) : null,
                    vehicleType != null && !vehicleType.isBlank()
                            ? com.pneumaliback.www.enums.VehicleType.valueOf(vehicleType)
                            : null,
                    imageUrl, description, categoryId, active);

            Product product = productService.updateFromRequest(id, request);
            return ResponseEntity.ok(product);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Format numérique invalide"));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Erreur lors de l'upload de l'image: " + e.getMessage()));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produit supprimé"),
            @ApiResponse(responseCode = "404", description = "Produit introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok(java.util.Map.of("message", "Produit supprimé avec succès"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/popular")
    @Operation(summary = "Produits populaires", description = "Les plus vendus")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> popular(Pageable pageable) {
        try {
            return ResponseEntity.ok(productService.popular(pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/brands")
    @Operation(summary = "Marques disponibles (actives)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des marques"),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> brands() {
        try {
            return ResponseEntity.ok(productService.brands());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/dimensions")
    @Operation(summary = "Recherche par dimensions", description = "Recherche par largeur, profil et diamètre (ex: 235 / 45 / 17)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Résultats récupérés"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> byDimensions(@RequestParam(required = false) String width,
            @RequestParam(required = false) String profile,
            @RequestParam(required = false) String diameter,
            Pageable pageable) {
        try {
            return ResponseEntity.ok(productService.findByDimensions(width, profile, diameter, pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
