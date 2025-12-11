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
    private String serviceRoleKeyRaw;

    private String serviceRoleKey;

    @jakarta.annotation.PostConstruct
    public void init() {
        // Nettoyer et valider la clé Supabase
        if (serviceRoleKeyRaw == null || serviceRoleKeyRaw.trim().isBlank()) {
            log.error("⚠️ SUPABASE_SERVICE_ROLE_KEY n'est pas définie ou est vide ! L'upload vers Supabase ne fonctionnera pas.");
            log.error("⚠️ Veuillez définir la variable d'environnement SUPABASE_SERVICE_ROLE_KEY dans Render avec la clé 'service_role' de votre projet Supabase.");
            serviceRoleKey = null;
        } else {
            // Nettoyer la clé (supprimer les espaces et retours à la ligne)
            String cleanedKey = serviceRoleKeyRaw.trim().replaceAll("\\s+", "");
            
            // Validation basique : un JWT doit contenir au moins deux points
            if (!cleanedKey.contains(".") || cleanedKey.split("\\.").length < 3) {
                log.error("⚠️ SUPABASE_SERVICE_ROLE_KEY semble invalide (format JWT attendu avec 3 parties séparées par des points)");
                log.error("⚠️ La clé doit être la 'service_role key' (pas l'anon key) de votre projet Supabase.");
                serviceRoleKey = null;
            } else {
                serviceRoleKey = cleanedKey;
                // Logger les 10 premiers caractères pour le debug (sans exposer la clé complète)
                String keyPreview = serviceRoleKey.length() > 10 
                    ? serviceRoleKey.substring(0, 10) + "..." 
                    : "***";
                log.info("✅ Clé Supabase configurée (preview: {}, longueur: {} caractères)", keyPreview, serviceRoleKey.length());
            }
        }
        
        // Valider également les autres paramètres
        if (supabaseUrl == null || supabaseUrl.trim().isBlank()) {
            log.warn("⚠️ SUPABASE_URL n'est pas définie");
        } else {
            log.info("✅ URL Supabase configurée: {}", supabaseUrl);
        }
        
        if (bucketName == null || bucketName.trim().isBlank()) {
            log.warn("⚠️ SUPABASE_BUCKET n'est pas défini");
        } else {
            log.info("✅ Bucket Supabase configuré: {}", bucketName);
        }
    }

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
        return uploadBytes(file.getBytes(), fileName, folder, file.getContentType());
    }

    public String uploadBytes(byte[] data, String fileName, String folder, String contentType) {
        validateSupabaseConfiguration();
        
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        String safeContentType = (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType;

        String safeFileName = fileName != null && !fileName.isBlank()
                ? fileName
                : generateUniqueFileName(null);

        String filePath = folder != null && !folder.isBlank()
                ? folder + "/" + safeFileName
                : safeFileName;

        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, filePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(safeContentType));
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(data, headers);

        try {
            log.debug("Tentative d'upload vers Supabase: bucket={}, filePath={}", bucketName, filePath);
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
                String errorBody = response.getBody() != null ? response.getBody() : "aucun détail";
                log.error("Échec de l'upload vers Supabase: status={}, body={}", response.getStatusCode(), errorBody);
                throw new RuntimeException("Échec de l'upload vers Supabase: " + response.getStatusCode() + " - " + errorBody);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorDetails = e.getResponseBodyAsString();
            log.error("Erreur HTTP lors de l'upload vers Supabase: status={}, message={}, body={}", 
                    e.getStatusCode(), e.getMessage(), errorDetails);
            
            if (e.getStatusCode().value() == 403 || e.getStatusCode().value() == 401) {
                throw new RuntimeException(
                        "Erreur d'authentification Supabase: vérifiez que SUPABASE_SERVICE_ROLE_KEY est correctement configurée dans les variables d'environnement (Render). " +
                        "Message: " + (errorDetails != null ? errorDetails : e.getMessage()), e);
            }
            throw new RuntimeException("Erreur lors de l'upload vers Supabase: " + e.getStatusCode() + " - " + errorDetails, e);
        } catch (Exception e) {
            log.error("Erreur lors de l'upload vers Supabase: url={}, bucket={}", uploadUrl, bucketName, e);
            throw new RuntimeException("Erreur lors de l'upload vers Supabase: " + e.getMessage(), e);
        }
    }

    private void validateSupabaseConfiguration() {
        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new IllegalStateException(
                    "Configuration Supabase invalide: SUPABASE_SERVICE_ROLE_KEY n'est pas définie ou est vide. " +
                    "Vérifiez les variables d'environnement dans Render.");
        }
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "Configuration Supabase invalide: SUPABASE_URL n'est pas définie. " +
                    "Vérifiez les variables d'environnement dans Render.");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException(
                    "Configuration Supabase invalide: SUPABASE_BUCKET n'est pas défini. " +
                    "Vérifiez les variables d'environnement dans Render.");
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

        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            log.warn("Impossible de supprimer le fichier {}: clé Supabase non configurée", filePath);
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
