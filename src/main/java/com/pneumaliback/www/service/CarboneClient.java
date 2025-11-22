package com.pneumaliback.www.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarboneClient {

    private final RestTemplate restTemplate;

    @Value("${app.carbone.api-url:https://render.carbone.io}")
    private String carboneApiUrl;

    @Value("${app.carbone.api-key:}")
    private String carboneApiKey;

    @Value("${app.carbone.template-id:}")
    private String templateId;

    public byte[] renderPdf(Map<String, Object> data) {
        validateConfiguration();

        String endpoint = String.format("%s/render/%s", carboneApiUrl, templateId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
        headers.setBearerAuth(carboneApiKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);
        payload.put("convertTo", "pdf");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    request,
                    byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Réponse invalide de Carbone (code: " + response.getStatusCode() + ")");
            }
            return response.getBody();
        } catch (RestClientResponseException ex) {
            log.error("Erreur Carbone ({}): {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Impossible de générer le PDF via Carbone", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Erreur inconnue lors de l'appel Carbone", ex);
        }
    }

    private void validateConfiguration() {
        if (carboneApiKey == null || carboneApiKey.isBlank()) {
            throw new IllegalStateException("La clé API Carbone n'est pas configurée");
        }
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalStateException("L'identifiant du template Carbone n'est pas configuré");
        }
    }
}
