package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.TireWidth;
import com.pneumaliback.www.entity.TireProfile;
import com.pneumaliback.www.entity.TireDiameter;
import com.pneumaliback.www.repository.TireWidthRepository;
import com.pneumaliback.www.repository.TireProfileRepository;
import com.pneumaliback.www.repository.TireDiameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class TireDimensionInitializationService implements CommandLineRunner {

    private final TireWidthRepository tireWidthRepository;
    private final TireProfileRepository tireProfileRepository;
    private final TireDiameterRepository tireDiameterRepository;

    private static final List<Integer> VALID_WIDTHS = Arrays.asList(155, 165, 175, 185, 195, 205, 215, 225, 235, 245,
            255);
    private static final List<Integer> VALID_PROFILES = Arrays.asList(30, 35, 40, 45, 50, 55, 60, 65, 70, 75);
    private static final List<Integer> VALID_DIAMETERS = Arrays.asList(13, 14, 15, 16, 17, 18, 19, 20);

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try {
            log.info("Début de l'initialisation des dimensions de pneus...");
            initializeWidths();
            initializeProfiles();
            initializeDiameters();
            log.info("Initialisation des dimensions de pneus terminée.");
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des dimensions de pneus", e);
        }
    }

    private void initializeWidths() {
        for (Integer value : VALID_WIDTHS) {
            tireWidthRepository.findByValue(value)
                    .orElseGet(() -> {
                        TireWidth width = new TireWidth();
                        width.setValue(value);
                        width.setActive(true);
                        TireWidth saved = tireWidthRepository.save(width);
                        log.debug("Largeur créée: {}", value);
                        return saved;
                    });
        }
        log.info("{} largeurs initialisées", VALID_WIDTHS.size());
    }

    private void initializeProfiles() {
        for (Integer value : VALID_PROFILES) {
            tireProfileRepository.findByValue(value)
                    .orElseGet(() -> {
                        TireProfile profile = new TireProfile();
                        profile.setValue(value);
                        profile.setActive(true);
                        TireProfile saved = tireProfileRepository.save(profile);
                        log.debug("Profil créé: {}", value);
                        return saved;
                    });
        }
        log.info("{} profils initialisés", VALID_PROFILES.size());
    }

    private void initializeDiameters() {
        for (Integer value : VALID_DIAMETERS) {
            tireDiameterRepository.findByValue(value)
                    .orElseGet(() -> {
                        TireDiameter diameter = new TireDiameter();
                        diameter.setValue(value);
                        diameter.setActive(true);
                        TireDiameter saved = tireDiameterRepository.save(diameter);
                        log.debug("Diamètre créé: {}", value);
                        return saved;
                    });
        }
        log.info("{} diamètres initialisés", VALID_DIAMETERS.size());
    }
}
