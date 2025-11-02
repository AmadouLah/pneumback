package com.pneumaliback.www.controller;

import com.pneumaliback.www.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Storage", description = "Gestion du stockage des fichiers")
@CrossOrigin(origins = "*")
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload un fichier", description = "Upload un fichier vers Supabase Storage")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le fichier est vide"));
            }

            String fileUrl = storageService.uploadFile(file, folder);
            return ResponseEntity.ok(Map.of(
                    "url", fileUrl,
                    "message", "Fichier uploadé avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Erreur lors de l'upload du fichier", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de l'upload du fichier"));
        }
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un fichier", description = "Supprime un fichier de Supabase Storage")
    public ResponseEntity<?> deleteFile(@RequestParam("path") String filePath) {
        try {
            storageService.deleteFile(filePath);
            return ResponseEntity.ok(Map.of("message", "Fichier supprimé avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du fichier", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la suppression du fichier"));
        }
    }
}
