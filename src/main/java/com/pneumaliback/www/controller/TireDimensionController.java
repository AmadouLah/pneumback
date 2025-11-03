package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.TireWidth;
import com.pneumaliback.www.entity.TireProfile;
import com.pneumaliback.www.entity.TireDiameter;
import com.pneumaliback.www.repository.TireWidthRepository;
import com.pneumaliback.www.repository.TireProfileRepository;
import com.pneumaliback.www.repository.TireDiameterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tire-dimensions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Dimensions de pneus", description = "API pour récupérer les dimensions de pneus prédéfinies")
public class TireDimensionController {

    private final TireWidthRepository tireWidthRepository;
    private final TireProfileRepository tireProfileRepository;
    private final TireDiameterRepository tireDiameterRepository;

    @GetMapping("/widths")
    @Operation(summary = "Récupérer toutes les largeurs actives")
    public ResponseEntity<List<TireWidth>> getWidths() {
        return ResponseEntity.ok(tireWidthRepository.findByActiveTrueOrderByValueAsc());
    }

    @GetMapping("/profiles")
    @Operation(summary = "Récupérer tous les profils actifs")
    public ResponseEntity<List<TireProfile>> getProfiles() {
        return ResponseEntity.ok(tireProfileRepository.findByActiveTrueOrderByValueAsc());
    }

    @GetMapping("/diameters")
    @Operation(summary = "Récupérer tous les diamètres actifs")
    public ResponseEntity<List<TireDiameter>> getDiameters() {
        return ResponseEntity.ok(tireDiameterRepository.findByActiveTrueOrderByValueAsc());
    }
}
