package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.repository.CategoryRepository;
import com.pneumaliback.www.service.ProductService;
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

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Produits", description = "Consultation et gestion des produits")
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            String msg = e.getMessage() != null ? e.getMessage() : "Requête invalide";
            if (msg.toLowerCase().contains("introuvable") || msg.toLowerCase().contains("non trouv")) {
                return ResponseEntity.status(404).body(java.util.Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", msg));
        }
        return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
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
            return ResponseEntity.ok(productService.findWithFilters(category, brand, size, season, minPrice, maxPrice, pageable));
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
    public ResponseEntity<?> create(@RequestBody Product product) {
        try {
            return ResponseEntity.ok(productService.save(product));
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

