package com.pneumaliback.www.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final RestTemplate restTemplate;

    @Value("${app.storage.supabase.url}")
    private String supabaseUrl;

    @Value("${app.storage.supabase.bucket}")
    private String bucketName;

    @Value("${app.storage.supabase.service-role-key}")
    private String serviceRoleKey;

    /**
     * Upload un fichier vers Supabase Storage
     * 
     * @param file   Le fichier à uploader
     * @param folder Le dossier dans lequel stocker le fichier (ex: "products")
     * @return L'URL publique du fichier uploadé
     * @throws IOException Si une erreur survient lors de la lecture du fichier
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Le nom du fichier est invalide");
        }

        String extension = getFileExtension(originalFilename);
        String fileName = generateUniqueFileName(extension);
        String filePath = folder != null && !folder.isBlank()
                ? folder + "/" + fileName
                : fileName;

        byte[] fileBytes = file.getBytes();
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, filePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String publicUrl = String.format("%s/storage/v1/object/public/%s/%s",
                        supabaseUrl, bucketName, filePath);
                log.info("Fichier uploadé avec succès: {}", publicUrl);
                return publicUrl;
            } else {
                log.error("Échec de l'upload: {}", response.getStatusCode());
                throw new RuntimeException("Échec de l'upload vers Supabase: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'upload vers Supabase", e);
            throw new RuntimeException("Erreur lors de l'upload vers Supabase: " + e.getMessage(), e);
        }
    }

    /**
     * Supprime un fichier de Supabase Storage
     * 
     * @param filePath Le chemin du fichier à supprimer (relatif au bucket)
     */
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            String deleteUrl = String.format("%s/storage/v1/object/%s/%s",
                    supabaseUrl, bucketName, filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceRoleKey);
            headers.set("apikey", serviceRoleKey);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, requestEntity, Void.class);
            log.info("Fichier supprimé avec succès: {}", filePath);
        } catch (Exception e) {
            log.warn("Erreur lors de la suppression du fichier {}: {}", filePath, e.getMessage());
        }
    }

    /**
     * Extrait le chemin du fichier depuis une URL Supabase
     * 
     * @param url L'URL complète du fichier
     * @return Le chemin relatif du fichier
     */
    public String extractFilePathFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String pattern = "/storage/v1/object/public/" + bucketName + "/";
        int index = url.indexOf(pattern);
        if (index != -1) {
            return url.substring(index + pattern.length());
        }
        return null;
    }

    /**
     * Génère un nom de fichier unique
     */
    private String generateUniqueFileName(String extension) {
        return UUID.randomUUID().toString() + (extension != null ? "." + extension : "");
    }

    /**
     * Extrait l'extension d'un fichier
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return null;
    }
}
